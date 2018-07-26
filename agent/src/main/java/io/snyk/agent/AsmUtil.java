package io.snyk.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;

/**
 * static utility class working around some ugliness in the ASM API
 */
public class AsmUtil {
    private static final int DEFAULT_PARSING_OPTIONS = 0;

    private AsmUtil() {
        throw new IllegalStateException();
    }

    static ClassNode parse(ClassReader reader) {
        final ClassNode cn = new ClassNode();
        reader.accept(cn, DEFAULT_PARSING_OPTIONS);
        return cn;
    }

    static byte[] byteArray(ClassNode cn) {
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    static Iterable<AbstractInsnNode> iterable(InsnList instructions) {
        return instructions::iterator;
    }
}
