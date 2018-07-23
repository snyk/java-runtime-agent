package io.snyk.agent;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

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
        if (isAbstract(method.access)
                || isNative(method.access)
                || isSynthetic(method.access)) {
            return false;
        }

        return true;
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
}
