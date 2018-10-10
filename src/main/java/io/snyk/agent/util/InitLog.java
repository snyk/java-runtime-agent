package io.snyk.agent.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Buffer initialisation logging in memory. When a real logger is available,
 * flush our stored data out to the real logger. If any more messages come
 * along, send them directly to the real logger.
 *
 * Needs to be thread safe, but not fast: everything is synchronized and static.
 */
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

    public static synchronized void flushToInstance(Log log) {
        instance = log;

        for (String initMessage : initMessages) {
            log.flushInitMessage(initMessage);
        }

        initMessages.clear();
    }
}
