package io.snyk.agent.jvm;

import io.snyk.agent.filter.FilterList;
import io.snyk.agent.logic.Config;
import io.snyk.agent.logic.DataTracker;
import io.snyk.agent.logic.FilterUpdate;
import io.snyk.agent.logic.ReportingWorker;
import io.snyk.agent.util.*;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The entry point for the agent. Load and install our plugins.
 */
class EntryPoint {
    public static void premain(
            String agentArguments,
            Instrumentation instrumentation) throws Exception {
        InitLog.loading("startup: " + Version.extendedVersionInfo());
        InitLog.loading("If you have any issues during this beta, please contact runtime@snyk.io");

        final File configFile = ConfigSearch.find(agentArguments);

        final Config config = Config.loadConfigFromFile(configFile.getAbsolutePath());

        final Log log;
        switch (config.logTo) {
            case FILE:
                log = new FileLog(configFile.getParentFile(), config.debugLoggingEnabled);
                break;
            case STDERR:
                log = new StdErrLog(config.debugLoggingEnabled);
                break;
            case NOWHERE:
                log = new NullLog();
                break;
            default:
                throw new IllegalStateException();
        }
        InitLog.flushToInstance(log);

        if (!config.skipBuiltInRules) {
            config.filters.set(FilterList.loadBuiltInFilters(log));
        }

        log.info("loading config complete, projectId:" + config.projectId);

        final CountDownLatch initialFetchComplete = new CountDownLatch(1);
        final Thread update = new Thread(new FilterUpdate(log,
                config,
                instrumentation,
                initialFetchComplete::countDown));
        update.setDaemon(true);
        update.setName("snyk-update");
        update.start();

        final DataTracker dataTracker = new DataTracker(log);

        // ReportingWorker's constructor gathers loads of system info, which
        // seems to take >5s on some systems (e.g. OSX), perhaps a failing DNS
        // query gathering the hostname?
        final Thread worker = new Thread(() -> new ReportingWorker(log, config, dataTracker).run());
        worker.setDaemon(true);
        worker.setName("snyk-agent");
        worker.start();

        final boolean canReTransform = true;
        instrumentation.addTransformer(new Transformer(log, config, dataTracker), canReTransform);

        if (!initialFetchComplete.await(config.filterUpdateInitialDelayMs, TimeUnit.MILLISECONDS)) {
            log.info("releasing agent as data refresh fetch timed out");
        } else {
            log.info("startup complete, releasing application");
        }
    }
}
