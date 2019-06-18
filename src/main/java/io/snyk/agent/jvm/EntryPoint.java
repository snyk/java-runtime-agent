package io.snyk.agent.jvm;

import io.snyk.agent.filter.FilterList;
import io.snyk.agent.logic.*;
import io.snyk.agent.util.*;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The entry point for the agent. Load and install our plugins.
 */
class EntryPoint {
    public static void agentmain(
            String arguments,
            Instrumentation instrumentation) throws Exception {
        beforeConfig();
        final ConfigBuilder builder = new ConfigBuilder();
        builder.projectId = "2d77851a-6716-419e-a95f-494f39f0cc10";
        builder.logTo = "stderr";
        final Config config = builder.build();
        initialise(instrumentation, config, null);
    }

    public static void premain(
            String agentArguments,
            Instrumentation instrumentation) throws Exception {
        beforeConfig();

        final File configFile = ConfigSearch.find(agentArguments);

        final Config config = Config.loadConfigFromFile(configFile.getAbsolutePath());
        final File logLocation = configFile.getParentFile();

        initialise(instrumentation, config, logLocation);
    }

    private static void beforeConfig() {
        InitLog.loading("startup: " + Version.extendedVersionInfo());
        InitLog.loading("If you have any issues during this beta, please contact runtime@snyk.io");
    }

    private static void initialise(Instrumentation instrumentation, Config config, File logLocation)
            throws MalformedURLException, InterruptedException {
        final Log log;
        switch (config.logTo) {
            case FILE:
                log = new FileLog(logLocation, config.debugLoggingEnabled);
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
