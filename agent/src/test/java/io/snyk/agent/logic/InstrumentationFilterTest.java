package io.snyk.agent.logic;

import io.snyk.agent.util.AsmUtil;
import io.snyk.agent.util.IterableJar;
import io.snyk.asm.ClassReader;
import io.snyk.asm.tree.ClassNode;
import io.snyk.asm.tree.MethodNode;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static io.snyk.agent.logic.InstrumentationFilter.bannedMethod;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InstrumentationFilterTest {
    @Test
    void methodsOfVictim() throws Exception {
        final String name = TestVictim.class.getName();
        final ClassNode node = AsmUtil.parse(new ClassReader(name));
        assertTrue(bannedMethod(findMethod(node, "getStringField")));
        assertTrue(bannedMethod(findMethod(node, "getIntField")));
        assertTrue(bannedMethod(findMethod(node, "setStringField")));
        assertTrue(bannedMethod(findMethod(node, "setIntField")));
        assertFalse(bannedMethod(findMethod(node, "call")));
        assertFalse(bannedMethod(findMethod(node, "localGeneric")));
        assertFalse(bannedMethod(findMethod(node, "returnLambda")));
    }

    @Test
    void findGuavaMethods() {
        for (ClassNode c : new IterableJar(() -> InstrumentationFilterTest.class.getResourceAsStream(
                "/guava-26.0-jre.jar"))) {
            for (MethodNode method : c.methods) {
                if (InstrumentationFilter.bannedMethod(method)) {
                    continue;
                }

                if (InstrumentationFilter.branches(c, method)) {
                    System.out.println(c.name + " // " + method.name + method.desc);
                }
            }
        }
    }

    public static MethodNode findMethod(ClassNode node, String name) throws NoSuchElementException {
        return node.methods.stream()
                .filter(method -> name.equals(method.name))
                .findFirst()
                .get();
    }
}
