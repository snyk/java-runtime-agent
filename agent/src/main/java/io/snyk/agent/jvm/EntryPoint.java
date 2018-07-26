package io.snyk.agent.jvm;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * The entry point for the agent. Load and install our plugins.
 */
public class EntryPoint {
    public static void premain(
            String agentArguments,
            Instrumentation instrumentation) throws UnmodifiableClassException {
        instrumentation.addTransformer(new Transformer(), false);
    }
}
