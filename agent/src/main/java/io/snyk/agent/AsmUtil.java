package io.snyk.agent;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/**
 * static utility class working around some ugliness in the ASM API
 */
public class AsmUtil {
    private AsmUtil() {
        throw new IllegalStateException();
    }

    static byte[] byteArray(ClassNode cn) {
        final ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        return cw.toByteArray();
    }

}
