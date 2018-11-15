package io.snyk.agent.util;

public class StdErrLog implements Log {
    private final boolean debugLoggingEnabled;

    public StdErrLog(boolean debugLoggingEnabled) {
        this.debugLoggingEnabled = debugLoggingEnabled;
    }

    @Override
    public void debug(String msg) {
        if (debugLoggingEnabled) {
            System.err.println(FileLog.makeLine("debug: " + msg));
        }
    }

    @Override
    public void info(String msg) {
        System.err.println(FileLog.makeLine(" info: " + msg));
    }

    @Override
    public void warn(String msg) {
        System.err.println(FileLog.makeLine(" warn: " + msg));

    }

    @Override
    public void stackTrace(Throwable e) {
        System.err.println(FileLog.makeLine(" exception"));
        e.printStackTrace();
    }

    @Override
    public void flushInitMessage(String initMessage) {
        // were already printed by `InitLog`
    }
}
