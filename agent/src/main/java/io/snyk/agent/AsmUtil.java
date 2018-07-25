package io.snyk.agent;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;

/**
 * static utility class working around some ugliness in the ASM API
 */
public class AsmUtil {
    private AsmUtil() {
        throw new IllegalStateException();
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
