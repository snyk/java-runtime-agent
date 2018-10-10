package io.snyk.agent.testutil;

import io.snyk.agent.util.Log;

public class TestLogger implements Log {
    public boolean loggedAnyExceptions = false;

    @Override
    public void debug(String msg) {
        System.err.println("test logging: debug: " + msg);
    }

    @Override
    public void info(String msg) {
        System.err.println("test logging:  info: " + msg);
    }

    @Override
    public void warn(String msg) {
        System.err.println("test logging:  warn: " + msg);
    }

    @Override
    public void stackTrace(Throwable e) {
        System.err.println("test logging: error:");
        e.printStackTrace(System.err);
        loggedAnyExceptions = true;
    }

    @Override
    public void flushInitMessage(String initMessage) {
        // we already printed them, no need to re-print
    }
}
