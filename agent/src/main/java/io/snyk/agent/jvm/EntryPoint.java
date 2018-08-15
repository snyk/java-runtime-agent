package io.snyk.agent.jvm;

import io.snyk.agent.logic.Config;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * The entry point for the agent. Load and install our plugins.
 */
public class EntryPoint {
    public static Config CONFIG;

    public static void premain(
            String agentArguments,
            Instrumentation instrumentation) throws UnmodifiableClassException {
        if (null == agentArguments || !agentArguments.startsWith("file:")) {
            throw new IllegalStateException("expected file:[path to config file]");
        }

        CONFIG = Config.fromFile(agentArguments.substring("file:".length()));

        System.err.println("snyk-agent: loading config complete, projectId:" + CONFIG.projectId);

        instrumentation.addTransformer(new Transformer(), false);
    }
}
