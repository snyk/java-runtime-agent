package io.snyk.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * perform rewrites of classes
 */
public class Rewriter {

    private static final int DEFAULT_PARSING_OPTIONS = 0;

    static byte[] rewrite(ClassReader reader) {
        final ClassNode cn = new ClassNode();
        reader.accept(cn, DEFAULT_PARSING_OPTIONS);
        for (MethodNode method : cn.methods) {
            if (Interesting.interesting(method)) {
                rewriteMethod(cn.name, method);
            }
        }
        return AsmUtil.byteArray(cn);
    }

    private static void rewriteMethod(String clazzInternalName, MethodNode method) {
        final String tag = clazzInternalName + ":" + method.name;
        final InsnList launchpad = new InsnList();
        launchpad.add(new LdcInsnNode(tag));
        launchpad.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Tracker.class.getName().replace('.', '/'),
                "registerCall", "(Ljava/lang/String;)V", false));
        method.instructions.insertBefore(method.instructions.getFirst(), launchpad);
    }
}
