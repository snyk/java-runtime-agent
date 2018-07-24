package io.snyk.demo;

public class Reflector {
    public void foo() {
        try {
            Reflector.class.getClassLoader().loadClass("foo");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
