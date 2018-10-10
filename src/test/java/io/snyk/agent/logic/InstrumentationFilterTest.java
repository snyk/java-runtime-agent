package io.snyk.agent.logic;

import io.snyk.agent.util.AsmUtil;
import io.snyk.agent.util.IterableJar;
import io.snyk.asm.ClassReader;
import io.snyk.asm.tree.ClassNode;
import io.snyk.asm.tree.MethodNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.NoSuchElementException;

import static io.snyk.agent.logic.InstrumentationFilter.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InstrumentationFilterTest {
    @ParameterizedTest(name = "[{0}] skip: {1} accessor: {2}, branches: {3}")
    @CsvSource({
            // method name,  skip,  isAcc, branches,
            "getStringField, false, true , false",
            "getIntField   , false, true , false",
            "setStringField, false, true , false",
            "setIntField   , false, true , false",
            "call          , false, false, false",
            "localGeneric  , false, false, false",
            // annoying one.. this is probably safe to skip, under the "branches" rule
            "returnLambda  , false, false, true ",
            "switch1       , false, false, true ",
            "switch2       , false, false, true ",

            // System.out is a static final field containing a PrintStream,
            // we virtually invoke println on this PrintStream.
            // Ironically this is correct, as System#setOut exists, but ideally
            // a flow control thing would claim this method was safe in this case
            // Probably not a super interesting one.
            "printInt      , false, false, true ",
            "printConcat   , false, false, true ",
            "concat        , false, false, false",
    })
    void methodsOfVictim(String method, boolean skip, boolean accessor, boolean branches) throws Exception {
        final String name = TestVictim.class.getName();
        final ClassNode classNode = AsmUtil.parse(new ClassReader(name));
        final MethodNode node = findMethod(classNode, method);

        assertEquals(skip, skipMethod(classNode, node), "skip");
        assertEquals(accessor, isAccessor(node), "accessor");
        assertEquals(branches, branches(classNode, node), "branches");
    }

    @Test
    void findGuavaMethods() {
        for (ClassNode c : new IterableJar(() -> InstrumentationFilterTest.class.getResourceAsStream(
                "/guava-26.0-jre.jar"))) {
            for (MethodNode method : c.methods) {
                if (InstrumentationFilter.skipMethod(c, method)) {
                    continue;
                }

                if (branches(c, method)) {
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
