package io.snyk.agent.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Log {

    private final boolean debugEnabled;
    private final PrintWriter logFile;

    // @GuardedBy("Log.class")
    private static List<String> initMessages = new ArrayList<>();
    // @GuardedBy("Log.class")
    private static Log instance = null;

    public static synchronized void loading(String msg) {
        if (null == instance) {
            final String line = makeLine("initialisation: " + msg);
            System.err.println(line);
            initMessages.add(line);
        } else {
            instance.info("initialisation: " + msg);
        }
    }

    public Log() {
        this(false);
    }

    public Log(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;

        Log.loading("switching to main logging...");

        try {
            logFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream("snyk-agent-" +
                    new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSS'Z'", Locale.US).format(new Date())
                    + ".log"), StandardCharsets.UTF_8));

            // note: must be last
            synchronized (Log.class) {
                instance = this;

                for (String initMessage : initMessages) {
                    logFile.println(initMessage);
                }

                initMessages.clear();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        info("logging enabled");
    }

    public void debug(String msg) {
        if (debugEnabled) {
            log("debug: " + msg);
        }
    }

    public void info(String msg) {
        log(" info: " + msg);
    }

    public void warn(String msg) {
        log(" warn: " + msg);
    }

    public void stackTrace(Throwable e) {
        e.printStackTrace(logFile);
        logFile.flush();
    }

    private void log(String msg) {
        final String line = makeLine(msg);
        logFile.println(line);
        logFile.flush();
        if (logFile.checkError()) {
            System.err.println("snyk-agent: fatal: can't write log file");
        }
    }

    private static String makeLine(String msg) {
        return Instant.now() + " snyk-agent " + msg;
    }
}
