package io.snyk.agent;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * Determine if a method is interesting enough to instrument.
 */
public class Interesting {
    static boolean interesting(String loadingClassAsName) {
        if (loadingClassAsName.startsWith("java/")) {
            return false;
        }

        if (loadingClassAsName.startsWith("sun/")) {
            return false;
        }

        if (loadingClassAsName.startsWith("com/sun/")) {
            return false;
        }

        if (loadingClassAsName.startsWith("javax/management/")) {
            return false;
        }

        return !loadingClassAsName.startsWith("io/snyk/agent/");
    }

    /**
     * Try and eliminate some methods we don't want to look at.
     * TODO: This is actually mandatory, as it enforces some asserts
     * TODO: that the actual rewriter should be enforcing, perhaps?
     */
    static boolean interesting(MethodNode method) {
        // `abstract` and `native` methods don't have Java code in them,
        // so we can't add extra java code
        // `synthetic` methods, used e.g. in generics dispatch internals,
        // probably don't make any sense to users, and don't contain any
        // non-generated code, so probably aren't going to usefully contain
        // an issue. They *could* be monitored, if wanted, Chris believes.
        if (isAbstract(method.access)
                || isNative(method.access)
                || isSynthetic(method.access)) {
            return false;
        }

        // We should expect that we don't have the full signature, just the descriptor;
        // which isn't too bad if we're ignoring synthetic methods.
        // Signatures which you'd think we could ignore, but probably can't:
        //  * `void foo()`: this is the internal type of constructors, maybe exclude those?

//        Type.getArgumentTypes(method.desc);
//        Type.getReturnType(method.desc);

        if (isAccessor(method)) {
            return false;
        }

        return true;
    }

    static boolean interesting(MethodInsnNode mi) {
        if (!returnsClass(mi)) {
            return false;
        }

        return true;
    }

    private static boolean isAccessor(MethodNode method) {
        if (isStatic(method.access)) {
            return false;
        }

        // e.g. constructors, initialisers
        if (method.name.startsWith("<")) {
            return false;
        }

        int args = Type.getArgumentTypes(method.desc).length;

        if (0 == args && isGetter(method)) {
            return true;
        }

        if (1 == args && isSetter(method)) {
            return true;
        }

        return false;
    }

    enum GetterParserState {
        ALoad0,
        GetField,
        Return,
    }

    private static boolean isGetter(MethodNode method) {
        GetterParserState state = GetterParserState.ALoad0;

        for (AbstractInsnNode node : AsmUtil.iterable(method.instructions)) {
            if (isNonsenseNode(node)) {
                continue;
            }

            switch (state) {
                case ALoad0:
                    if (isALoad(node, 0)) {
                        state = GetterParserState.GetField;
                        break;
                    } else {
                        return false;
                    }
                case GetField:
                    if (node instanceof FieldInsnNode) {
                        // TODO: this matches fields of other classes. Is that a problem?
                        state = GetterParserState.Return;
                        break;
                    } else {
                        return false;
                    }
                case Return:
                    return isReturn(node);
            }
        }
        return false;
    }

    enum SetterParserState {
        ALoad0,
        ALoad1,
        PutField,
        Return,
    }

    private static boolean isSetter(MethodNode method) {
        SetterParserState state = SetterParserState.ALoad0;

        for (AbstractInsnNode node : AsmUtil.iterable(method.instructions)) {
            if (isNonsenseNode(node)) {
                continue;
            }

            switch (state) {
                case ALoad0:
                    if (isALoad(node, 0)) {
                        state = SetterParserState.ALoad1;
                        break;
                    } else {
                        return false;
                    }
                case ALoad1:
                    if (isALoad(node, 1)) {
                        state = SetterParserState.PutField;
                        break;
                    } else {
                        return false;
                    }
                case PutField:
                    if (node instanceof FieldInsnNode) {
                        // TODO: this matches fields of other classes. Is that a problem?
                        state = SetterParserState.Return;
                        break;
                    } else {
                        return false;
                    }
                case Return:
                    return isReturn(node);
            }
        }
        return false;
    }

    private static boolean isALoad(AbstractInsnNode node, int of) {
        return node instanceof VarInsnNode && of == ((VarInsnNode) node).var;
    }

    private static boolean isNonsenseNode(AbstractInsnNode node) {
        return (node.getType() == AbstractInsnNode.LINE)
                || (node.getType() == AbstractInsnNode.FRAME)
                || (node.getType() == AbstractInsnNode.LABEL);
    }

    private static boolean isReturn(AbstractInsnNode node) {
        switch (node.getOpcode()) {
            case Opcodes.RETURN: return true;
            case Opcodes.IRETURN: return true;
            case Opcodes.LRETURN: return true;
            case Opcodes.FRETURN: return true;
            case Opcodes.DRETURN: return true;
            case Opcodes.ARETURN: return true;
            default: return false;
        }
    }

    private static boolean returnsClass(MethodInsnNode mi) {
        final Type returnType = Type.getReturnType(mi.desc);

        // getInternalName only valid for `Object` `sort`s
        if (Type.OBJECT != returnType.getSort()) {
            return false;
        }

        // https://quad.pe/e/EM0TwLHCzH.png
        switch (returnType.getInternalName()) {
            case "java/lang/Class": return true;
            case "java/lang/ClassLoader": return true;
            default: return false;
        }
    }

    private static boolean isSynthetic(int access) {
        return Opcodes.ACC_SYNTHETIC == (access & Opcodes.ACC_SYNTHETIC);
    }

    private static boolean isAbstract(int access) {
        return Opcodes.ACC_ABSTRACT == (access & Opcodes.ACC_ABSTRACT);
    }

    private static boolean isNative(int access) {
        return Opcodes.ACC_NATIVE == (access & Opcodes.ACC_NATIVE);
    }

    private static boolean isStatic(int access) {
        return Opcodes.ACC_STATIC == (access & Opcodes.ACC_STATIC);
    }
}
