package io.snyk.agent.logic;

import io.snyk.agent.util.InsnIter;
import io.snyk.asm.Opcodes;
import io.snyk.asm.Type;
import io.snyk.asm.tree.*;

import java.util.HashSet;
import java.util.ListIterator;

/**
 * Determine if a method is interesting enough to instrument.
 */
public class InstrumentationFilter {
    private static final HashSet<String> FAMOUSLY_FINAL = new HashSet<>();

    static {
        // critical due to string concatenation
        FAMOUSLY_FINAL.add("java/lang/String");
        FAMOUSLY_FINAL.add("java/lang/StringBuilder");

        // other potentially widely used "data types"
        FAMOUSLY_FINAL.add("java/lang/URL");
        FAMOUSLY_FINAL.add("java/lang/URI");
        FAMOUSLY_FINAL.add("java/lang/UUID");
        FAMOUSLY_FINAL.add("java/util/regex/Pattern");
        FAMOUSLY_FINAL.add("java/time/LocalTime");
        FAMOUSLY_FINAL.add("java/time/LocalDate");
        FAMOUSLY_FINAL.add("java/time/Instant");
        FAMOUSLY_FINAL.add("java/time/Duration");

        // e.g. getEnv() .. wait, no, that's static; printing is field access, uh.
        FAMOUSLY_FINAL.add("java/lang/System");
        FAMOUSLY_FINAL.add("java/lang/Math");
        FAMOUSLY_FINAL.add("java/util/Spliterators");
        FAMOUSLY_FINAL.add("java/util/Scanner");
        FAMOUSLY_FINAL.add("java/nio/ByteOrder");
        FAMOUSLY_FINAL.add("nio/channels/Channels");

        FAMOUSLY_FINAL.add("java/util/Optional");
        FAMOUSLY_FINAL.add("java/util/OptionalLong");

        // critical due to boxing
        FAMOUSLY_FINAL.add("java/lang/Boolean");
        FAMOUSLY_FINAL.add("java/lang/Character");
        FAMOUSLY_FINAL.add("java/lang/Double");
        FAMOUSLY_FINAL.add("java/lang/Float");
        FAMOUSLY_FINAL.add("java/lang/Integer");
        FAMOUSLY_FINAL.add("java/lang/Long");
        FAMOUSLY_FINAL.add("java/lang/Short");

        // Self-introspection (i.e. resource loading)
        FAMOUSLY_FINAL.add("java/lang/Class");


        // Notes:
        // * Guava Immutable* are all *abstract*, not even close to final
        // * "Effectively final" classes (e.g. private constructors) are interesting, but can't be found without
        //     introspection. Also, typically, "utility" classes only have static methods.
        // * None of the older serialisation infra is final, but we probably wouldn't want to exclude it anyway
        // * Map#Entry is an interface, which is a shame, as it gets used so much for iteration etc.
    }

    public static boolean bannedClassName(String loadingClassAsName) {
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
    static boolean bannedMethod(MethodNode method) {
        // `abstract` and `native` methods don't have Java code in them,
        // so we can't add extra java code
        // `synthetic` methods, used e.g. in generics dispatch internals,
        // probably don't make any sense to users, and don't contain any
        // non-generated code, so probably aren't going to usefully contain
        // an issue. They *could* be monitored, if wanted, Chris believes.
        if (isAbstract(method.access)
                || isNative(method.access)
                || isSynthetic(method.access)) {
            return true;
        }

        // exclude class initialisers. Unfortunately, this doesn't actually stop
        // us running code at class initialisation time, because initialisers can and will
        // call static methods, which we will still rewrite. Maybe we should try and trace
        // when methods are called from <clinit>?
        if (method.name.startsWith("<clinit")) {
            return true;
        }

        // We should expect that we don't have the full signature, just the descriptor;
        // which isn't too bad if we're ignoring synthetic methods.
        // Signatures which you'd think we could ignore, but probably can't:
        //  * `void foo()`: this is the internal type of constructors, maybe exclude those?

//        Type.getArgumentTypes(method.desc);
//        Type.getReturnType(method.desc);

        if (isAccessor(method)) {
            return true;
        }

        return false;
    }

    static boolean branches(ClassNode self, MethodNode method) {
        if (!method.tryCatchBlocks.isEmpty()) {
            return true;
        }

        final ListIterator<AbstractInsnNode> insns = method.instructions.iterator();
        while (insns.hasNext()) {
            final AbstractInsnNode insn = insns.next();

            final int type = insn.getType();

            if (type == AbstractInsnNode.JUMP_INSN
                    || type == AbstractInsnNode.INVOKE_DYNAMIC_INSN
                    || type == AbstractInsnNode.LOOKUPSWITCH_INSN
                    || type == AbstractInsnNode.TABLESWITCH_INSN) {
                return true;
            }

            if (insn instanceof MethodInsnNode) {
                if (!isStaticEnoughCall(self, (MethodInsnNode) insn)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isStaticEnoughCall(ClassNode self, MethodInsnNode call) {
        if (Opcodes.INVOKESTATIC == call.getOpcode() || Opcodes.INVOKESPECIAL == call.getOpcode()) {
            return true;
        }

        // any dynamic dispatch (interface, virtual, special (i.e. constructor))

        // it would be way better if we could check if the class was actually final,
        // but I don't like the risk tradeoff (of loading it and introspecting it),
        // plus it changing during runtime (very, very unlikely).
        if (FAMOUSLY_FINAL.contains(call.owner)) {
            return true;
        }

        // this isn't important if we're allowing INVOKESPECIAL above, as the compiler (always?) replaces these
        // with invoke INVOKESPECIAL. (always?)
        if (false && call.owner.equals(self.name)) {
            // if it's a method call on this object, and the target method is private,
            // then it's effectively a static method call
            final boolean resolvedToPrivateMethod = self.methods.stream()
                    .filter(method -> method.name.equals(call.name) &&
                            method.desc.equals(call.desc) &&
                            !isSynthetic(method.access))
                    .findAny()
                    .map(method -> isPrivate(method.access))
                    .orElse(false);

            if (resolvedToPrivateMethod) {
                return true;
            }
        }
        return false;
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

    private static boolean isGetter(MethodNode method) {
        final InsnIter iter = new InsnIter(method.instructions);

        return iter.nextIs(node -> isALoad(node, 0))
                // TODO: this matches fields of other classes. Is that a problem?
                && iter.nextIs(node -> node instanceof FieldInsnNode)
                && iter.nextIs(InstrumentationFilter::isReturn);
    }

    private static boolean isSetter(MethodNode method) {
        final InsnIter iter = new InsnIter(method.instructions);

        return iter.nextIs(node -> isALoad(node, 0))
                && iter.nextIs(node -> isALoad(node, 1))
                // TODO: this matches fields of other classes. Is that a problem?
                && iter.nextIs(node -> node instanceof FieldInsnNode)
                && iter.nextIs(InstrumentationFilter::isReturn);
    }

    private static boolean isALoad(AbstractInsnNode node, int of) {
        return node instanceof VarInsnNode && of == ((VarInsnNode) node).var;
    }

    public static boolean isNonsenseNode(AbstractInsnNode node) {
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

    static boolean returnsClass(MethodInsnNode mi) {
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

    private static boolean isPrivate(int access) {
        return Opcodes.ACC_PRIVATE == (access & Opcodes.ACC_PRIVATE);
    }
}
