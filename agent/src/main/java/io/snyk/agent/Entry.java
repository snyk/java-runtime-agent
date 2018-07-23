package io.snyk.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class Entry {
    public static void premain(String agentArguments, Instrumentation instrumentation) throws UnmodifiableClassException {
        instrumentation.addTransformer(new Rewrite(), true);
        try {
            ClassLoader.getSystemClassLoader().loadClass(Rewrite.class.getName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Couldn't find ourselves");
        }
    }
}
