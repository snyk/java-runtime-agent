package io.snyk.agent;

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

        return true;
    }

    static boolean interesting(MethodNode method) {
        return true;
    }
}
