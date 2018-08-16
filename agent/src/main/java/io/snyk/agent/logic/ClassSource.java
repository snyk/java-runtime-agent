package io.snyk.agent.logic;

public class ClassSource {
    public void observe(ClassLoader loader, String className) {
        System.err.println(
                loader.getResource(className + ".class").toString()
        );
    }
}
