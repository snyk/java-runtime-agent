package io.snyk.agent.testutil;

import io.snyk.agent.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLogger implements Log {
    private static final Logger logger = LoggerFactory.getLogger(TestLogger.class);

    public boolean loggedAnyExceptions = false;

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void stackTrace(Throwable e) {
        logger.error("exception", e);
        loggedAnyExceptions = true;
    }

    @Override
    public void flushInitMessage(String initMessage) {
        // we already printed them, no need to re-print
    }
}
