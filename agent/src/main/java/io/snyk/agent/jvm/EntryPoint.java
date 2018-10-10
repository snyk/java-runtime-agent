package io.snyk.agent.jvm;

import io.snyk.agent.logic.ClassSource;
import io.snyk.agent.logic.Config;
import io.snyk.agent.logic.ReportingWorker;
import io.snyk.agent.util.Log;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.MalformedURLException;

/**
 * The entry point for the agent. Load and install our plugins.
 */
class EntryPoint {
    public static void premain(
            String agentArguments,
            Instrumentation instrumentation) throws MalformedURLException {
        Log.loading("startup: " + Version.extendedVersionInfo());

        final File configFile = ConfigSearch.find(agentArguments);

        if (null == configFile) {
            throw new IllegalStateException("config file not found");
        }

        final Config config = Config.fromFile(configFile.getAbsolutePath());

        final Log log = new Log(configFile.getParentFile(), config.debugLoggingEnabled);

        log.info("loading config complete, projectId:" + config.projectId);

        final ClassSource classSource = new ClassSource(log);

        final Thread worker = new Thread(new ReportingWorker(log, config, classSource));
        worker.setDaemon(true);
        worker.setName("snyk-agent");
        worker.start();

        instrumentation.addTransformer(new Transformer(log, config, classSource), false);
    }
}
