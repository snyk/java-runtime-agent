package io.snyk.agent.csum;

import io.snyk.agent.logic.InstrumentationFilterTest;
import io.snyk.agent.logic.TestVictim;
import io.snyk.agent.util.AsmUtil;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MethodIdTest {

    @Test
    void testVictim() throws IOException {
        assertEquals(0x0a554076, hashTestMethod("switch1"));
        assertEquals(0x366fc085, hashTestMethod("switch2"));
    }

    @Test
    void testSmokeGuava() throws IOException {
        final JarInputStream jar = new JarInputStream(MethodIdTest.class.getResourceAsStream(
                "/guava-26.0-jre.jar"));
        JarEntry entry;
        final Map<Integer, Set<String>> seen = new HashMap<>();
        int running = 0;
        int totalMethods = 0;
        while (null != (entry = jar.getNextJarEntry())) {
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }
            for (MethodNode method : AsmUtil.parse(new ClassReader(jar)).methods) {
                final int computed = MethodId.id(method);
                running += computed;
                totalMethods += 1;
                seen.computeIfAbsent(computed, _id -> new HashSet<>())
                        .add(entry.getName() + " // " + method.name + method.desc);
            }
        }

        int collisions = 0;

        for (Map.Entry<Integer, Set<String>> idNames : seen.entrySet()) {
            final Set<String> methods = idNames.getValue();
            if (methods.size() == 1) {
                continue;
            }

            collisions += 1;

            System.out.println(idNames.getKey());
            for (String method : methods) {
                System.out.println(" * " + method);
            }
            System.out.println();
        }

        assertEquals(15669, totalMethods);
        assertEquals(505, collisions);
        assertEquals(-1180639864, running);
    }

    private int hashTestMethod(String name) throws IOException {
        return MethodId.id(
                InstrumentationFilterTest.findMethod(AsmUtil.parse(new ClassReader(TestVictim.class.getName())), name)
        );
    }
}