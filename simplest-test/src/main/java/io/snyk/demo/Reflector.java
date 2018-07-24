package io.snyk.demo;

public class Reflector {
    public void foo() {
        try {
            Reflector.class.getClassLoader().loadClass("foo");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            new NewLoader().bar();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

class NewLoader extends ClassLoader {
    void bar() throws ClassNotFoundException {
        loadClass("bar", true);
    }
}
