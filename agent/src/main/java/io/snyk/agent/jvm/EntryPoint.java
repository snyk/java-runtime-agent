package io.snyk.agent.jvm;

import io.snyk.agent.logic.ClassSource;
import io.snyk.agent.logic.Config;
import io.snyk.agent.logic.ReportingWorker;
import io.snyk.agent.util.Log;

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

        final Log log = new Log();

        final Config config = Config.fromFile(agentArguments.substring("file:".length()));

        log.info("loading config complete, projectId:" + config.projectId);

        final ClassSource classSource = new ClassSource(log);

        final Thread worker = new Thread(new ReportingWorker(log, config, classSource));
        worker.setDaemon(true);
        worker.setName("snyk-agent");
        worker.start();

        instrumentation.addTransformer(new Transformer(log, config, classSource), false);
    }
}
