package io.snyk.agent;


import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RewriterTest {
    @Test
    public void smokeTest() throws Exception {
        final String name = TestVictim.class.getName();
        final byte[] bytes = new Rewriter(Tracker.class, Tracker.SEEN_SET).rewrite(new ClassReader(name));
        final Class<?> clazz = new DefinerLoader().define(name, bytes);
        final Object instance = clazz.newInstance();
        assertEquals(5, clazz.getDeclaredMethod("returnFive").invoke(instance));
    }
}

class DefinerLoader extends ClassLoader {
    Class<?> define(String name, byte[] bytes) {
        return this.defineClass(name, bytes, 0, bytes.length);
    }
}
