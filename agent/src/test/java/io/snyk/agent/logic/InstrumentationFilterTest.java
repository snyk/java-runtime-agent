package io.snyk.agent.logic;

import io.snyk.agent.util.AsmUtil;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.NoSuchElementException;

import static io.snyk.agent.logic.InstrumentationFilter.interestingMethod;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstrumentationFilterTest {
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
        // casting for 5.2 compat; fixed in ASM 6
        return ((List<MethodNode>)node.methods).stream()
                .filter(method -> name.equals(method.name))
                .findFirst()
                .get();
    }
}
