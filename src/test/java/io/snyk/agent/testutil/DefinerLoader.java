package io.snyk.agent.testutil;

/**
 * Give access to `defineClass` directly. This isn't exposed because
 * you need to know what you're doing, or you cause all kinds of leaks
 * and conflicts. We don't care, as this is just for testing.
 */
public class DefinerLoader extends ClassLoader {
    public Class<?> define(String name, byte[] bytes) {
        return this.defineClass(name, bytes, 0, bytes.length);
    }
}
