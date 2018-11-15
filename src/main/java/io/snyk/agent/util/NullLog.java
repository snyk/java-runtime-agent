package io.snyk.agent.util;

public class NullLog implements Log {
    @Override
    public void debug(String msg) {
    }

    @Override
    public void info(String msg) {
    }

    @Override
    public void warn(String msg) {
    }

    @Override
    public void stackTrace(Throwable e) {
    }

    @Override
    public void flushInitMessage(String initMessage) {
    }
}
