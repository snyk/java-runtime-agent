package io.snyk.agent.util;

public class Log {
    public static void loading(String msg) {
        System.err.println("snyk-agent initialisation: " + msg);
    }

    public void info(String msg) {
        System.err.println("snyk-agent info: " + msg);
    }

    public void warn(String msg) {
        System.err.println("snyk-agent warning: " + msg);
    }
}
