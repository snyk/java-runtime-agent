package io.snyk.agent.csum;

import io.snyk.agent.logic.InstrumentationFilterTest;
import io.snyk.agent.logic.TestVictim;
import io.snyk.agent.util.AsmUtil;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MethodIdTest {

    @Test
    public void testVictim() throws IOException {
        assertEquals(0x0a554076, hashTestMethod("switch1"));
        assertEquals(0x366fc085, hashTestMethod("switch2"));
    }

    private int hashTestMethod(String name) throws IOException {
        return new MethodId().id(
                InstrumentationFilterTest.findMethod(AsmUtil.parse(new ClassReader(TestVictim.class.getName())), name)
        );
    }
}