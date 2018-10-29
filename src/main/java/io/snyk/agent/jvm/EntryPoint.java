package io.snyk.agent.jvm;

import io.snyk.agent.logic.Config;
import io.snyk.agent.logic.DataTracker;
import io.snyk.agent.logic.ReportingWorker;
import io.snyk.agent.util.FileLog;
import io.snyk.agent.util.InitLog;
import io.snyk.agent.util.Log;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;

/**
 * The entry point for the agent. Load and install our plugins.
 */
class EntryPoint {
    public static void premain(
            String agentArguments,
            Instrumentation instrumentation) throws MalformedURLException {
        InitLog.loading("startup: " + Version.extendedVersionInfo());

        final File configFile = ConfigSearch.find(agentArguments);

        final Config config = Config.fromFileWithDefault(configFile.getAbsolutePath());

        final Log log = new FileLog(configFile.getParentFile(), config.debugLoggingEnabled);
        InitLog.flushToInstance(log);

        log.info("loading config complete, projectId:" + config.projectId);

        final DataTracker dataTracker = new DataTracker(log);

        final Thread worker = new Thread(new ReportingWorker(log, config, dataTracker));
        worker.setDaemon(true);
        worker.setName("snyk-agent");
        worker.start();

        instrumentation.addTransformer(new Transformer(log, config, dataTracker), false);
    }
}
