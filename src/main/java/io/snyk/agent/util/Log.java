package io.snyk.agent.util;

public interface Log {
    void debug(String msg);

    void info(String msg);

    void warn(String msg);

    void stackTrace(Throwable e);

    void flushInitMessage(String initMessage);
}
