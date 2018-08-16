package io.snyk.agent.jvm;

import io.snyk.agent.logic.ClassSource;
import io.snyk.agent.logic.Config;
import io.snyk.agent.logic.ReportingWorker;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * The entry point for the agent. Load and install our plugins.
 */
public class EntryPoint {
    public static void premain(
            String agentArguments,
            Instrumentation instrumentation) throws UnmodifiableClassException {
        if (null == agentArguments || !agentArguments.startsWith("file:")) {
            throw new IllegalStateException("expected file:[path to config file]");
        }

        final Config config = Config.fromFile(agentArguments.substring("file:".length()));

        System.err.println("snyk-agent: loading config complete, projectId:" + config.projectId);

        final ClassSource classSource = new ClassSource();

        final Thread worker = new Thread(new ReportingWorker(config, classSource));
        worker.setDaemon(true);
        worker.setName("snyk-agent");
        worker.start();

        instrumentation.addTransformer(new Transformer(classSource), false);
    }
}
