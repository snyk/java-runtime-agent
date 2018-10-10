package io.snyk.agent.util;

import java.util.ArrayList;
import java.util.List;

public class InitLog {

    // @GuardedBy("Log.class")
    private static List<String> initMessages = new ArrayList<>();
    // @GuardedBy("Log.class")
    private static Log instance = null;

    public static synchronized void loading(String msg) {
        if (null == instance) {
            final String line = FileLog.makeLine("initialisation: " + msg);
            System.err.println(line);
            initMessages.add(line);
        } else {
            instance.info("initialisation: " + msg);
        }
    }

    // @VisibleForTesting
    public static synchronized void setInstance(Log log) {
        instance = log;

        for (String initMessage : initMessages) {
            log.flushInitMessage(initMessage);
        }

        initMessages.clear();
    }
}
