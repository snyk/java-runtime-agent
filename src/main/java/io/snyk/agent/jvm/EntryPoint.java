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
    public static void main(String... args) throws Exception {
        if (2 == args.length && "network-test".equals(args[0])) {
            NetworkTest.run(args[1]);
            return;
        }

        System.err.println("This is not an executable jar.");
        System.err.println();
        System.err.println("Please refer to the installation instructions.");
    }

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
            config.filters.set(FilterList.loadBuiltInBolos(log));
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

        final Thread worker = new Thread(new ReportingWorker(log, config, dataTracker));
        worker.setDaemon(true);
        worker.setName("snyk-agent");
        worker.start();

        final boolean canReTransform = true;
        instrumentation.addTransformer(new Transformer(log, config, dataTracker), canReTransform);

        if (!initialFetchComplete.await(config.filterUpdateInitialDelayMs, TimeUnit.MILLISECONDS)) {
            log.warn("releasing agent as data refresh fetch timed out");
        } else {
            log.info("startup complete, releasing application");
        }
    }
}
