package io.snyk.agent.util;

import io.snyk.asm.ClassReader;
import io.snyk.asm.ClassWriter;
import io.snyk.asm.tree.AbstractInsnNode;
import io.snyk.asm.tree.ClassNode;
import io.snyk.asm.tree.InsnList;

/**
 * static utility class working around some ugliness in the ASM API
 */
public class AsmUtil {
    private static final int DEFAULT_PARSING_OPTIONS = 0;

    private AsmUtil() {
        throw new IllegalStateException();
    }

    public static ClassNode parse(ClassReader reader) {
        final ClassNode cn = new ClassNode();
        reader.accept(cn, DEFAULT_PARSING_OPTIONS);
        return cn;
    }

    public static byte[] byteArray(ClassNode cn) {
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    static Iterable<AbstractInsnNode> iterable(InsnList instructions) {
        return instructions::iterator;
    }
}
