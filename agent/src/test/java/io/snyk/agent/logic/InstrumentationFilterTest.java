package io.snyk.agent.logic;

import io.snyk.agent.util.AsmUtil;
import io.snyk.agent.util.IterableJar;
import io.snyk.asm.ClassReader;
import io.snyk.asm.tree.ClassNode;
import io.snyk.asm.tree.MethodNode;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static io.snyk.agent.logic.InstrumentationFilter.skipMethod;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InstrumentationFilterTest {
    @Test
    void methodsOfVictim() throws Exception {
        final String name = TestVictim.class.getName();
        final ClassNode node = AsmUtil.parse(new ClassReader(name));
        assertTrue(skipMethod(node, findMethod(node, "getStringField")));
        assertTrue(skipMethod(node, findMethod(node, "getIntField")));
        assertTrue(skipMethod(node, findMethod(node, "setStringField")));
        assertTrue(skipMethod(node, findMethod(node, "setIntField")));
        assertTrue(skipMethod(node, findMethod(node, "call")));
        assertTrue(skipMethod(node, findMethod(node, "localGeneric")));

        // annoying one.. this is probably safe to skip, under the "branches" rule
        assertFalse(skipMethod(node, findMethod(node, "returnLambda")));

        assertFalse(skipMethod(node, findMethod(node, "switch1")));
        assertFalse(skipMethod(node, findMethod(node, "switch2")));

        // System.out is a static final field containing a PrintStream,
        // we virtually invoke println on this PrintStream.
        // Ironically this is correct, as System#setOut exists, but ideally
        // a flow control thing would claim this method was safe in this case
        // Probably not a super interesting one.
        assertFalse(skipMethod(node, findMethod(node, "printInt")));
        assertFalse(skipMethod(node, findMethod(node, "printConcat")));

        assertTrue(skipMethod(node, findMethod(node, "concat")));
    }

    @Test
    void findGuavaMethods() {
        for (ClassNode c : new IterableJar(() -> InstrumentationFilterTest.class.getResourceAsStream(
                "/guava-26.0-jre.jar"))) {
            for (MethodNode method : c.methods) {
                if (InstrumentationFilter.skipMethod(c, method)) {
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
