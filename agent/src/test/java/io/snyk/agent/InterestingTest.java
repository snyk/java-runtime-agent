package io.snyk.agent;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.NoSuchElementException;

import static io.snyk.agent.Interesting.interestingMethod;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterestingTest {
    @Test
    void methodsOfVictim() throws Exception {
        final String name = TestVictim.class.getName();
        final ClassNode node = AsmUtil.parse(new ClassReader(name));
        assertFalse(interestingMethod(findMethod(node, "getStringField")));
        assertFalse(interestingMethod(findMethod(node, "getIntField")));
        assertFalse(interestingMethod(findMethod(node, "setStringField")));
        assertFalse(interestingMethod(findMethod(node, "setIntField")));
        assertTrue(interestingMethod(findMethod(node, "call")));
        assertTrue(interestingMethod(findMethod(node, "localGeneric")));
        assertTrue(interestingMethod(findMethod(node, "returnLambda")));
    }

    private MethodNode findMethod(ClassNode node, String name) throws NoSuchElementException {
        return node.methods.stream()
                .filter(method -> name.equals(method.name))
                .findFirst()
                .get();
    }
}
