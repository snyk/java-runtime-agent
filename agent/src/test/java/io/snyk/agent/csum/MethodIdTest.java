package io.snyk.agent.csum;

import io.snyk.agent.logic.InstrumentationFilterTest;
import io.snyk.agent.logic.TestVictim;
import io.snyk.agent.util.AsmUtil;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MethodIdTest {

    @Test
    public void testVictim() throws IOException {
        assertEquals(0x0a554076, hashTestMethod("switch1"));
        assertEquals(0x366fc085, hashTestMethod("switch2"));
    }

    @Test
    void testSmokeGuava() throws IOException {
        final JarInputStream jar = new JarInputStream(MethodIdTest.class.getResourceAsStream(
                "/guava-26.0-jre.jar"));
        JarEntry entry;
        while (null != (entry = jar.getNextJarEntry())) {
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }
            for (MethodNode method : AsmUtil.parse(new ClassReader(jar)).methods) {
                new MethodId().id(method);
            }
        }
    }

    private int hashTestMethod(String name) throws IOException {
        return new MethodId().id(
                InstrumentationFilterTest.findMethod(AsmUtil.parse(new ClassReader(TestVictim.class.getName())), name)
        );
    }
}
