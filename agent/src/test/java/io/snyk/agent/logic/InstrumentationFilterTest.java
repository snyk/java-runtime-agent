package io.snyk.agent.logic;

import io.snyk.agent.util.AsmUtil;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
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

    public static MethodNode findMethod(ClassNode node, String name) throws NoSuchElementException {
        // casting for 5.2 compat; fixed in ASM 6
        return ((List<MethodNode>)node.methods).stream()
                .filter(method -> name.equals(method.name))
                .findFirst()
                .get();
    }
}
