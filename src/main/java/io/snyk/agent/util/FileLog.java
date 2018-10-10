package io.snyk.agent.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

public class FileLog implements Log {
    private final boolean debugEnabled;
    private final PrintWriter logFile;

    public FileLog(File baseDir, boolean debugEnabled) {
        this.debugEnabled = debugEnabled;

        final File logDir = new File(baseDir, "snyk-logs");
        if (!logDir.isDirectory() && !logDir.mkdirs()) {
            throw new IllegalStateException("invalid log dir: " + logDir);
        }

        final File logPath = new File(logDir,
                "agent-" + new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSS'Z'", Locale.US).format(new Date()) + ".log");

        InitLog.loading("switching logging to " + logPath.getAbsolutePath());

        try {
            this.logFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logPath),
                    StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        info("logging enabled");
    }


    @Override
    public void debug(String msg) {
        if (debugEnabled) {
            log("debug: " + msg);
        }
    }

    @Override
    public void info(String msg) {
        log(" info: " + msg);
    }

    @Override
    public void warn(String msg) {
        log(" warn: " + msg);
    }

    @Override
    public void stackTrace(Throwable e) {
        e.printStackTrace(logFile);
        logFile.flush();
    }

    @Override
    public void flushInitMessage(String initMessage) {
        logFile.println(initMessage);
    }

    private void log(String msg) {
        final String line = makeLine(msg);
        logFile.println(line);
        logFile.flush();
        if (logFile.checkError()) {
            System.err.println("snyk-agent: fatal: can't write log file");
        }
    }

    static String makeLine(String msg) {
        return Instant.now() + " snyk-agent " + msg;
    }
}
