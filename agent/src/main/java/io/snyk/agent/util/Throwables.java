package io.snyk.agent.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Throwables {
    public static String getStackTrace(Throwable t) {
        try (final StringWriter sw = new StringWriter();
             final PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
            return sw.toString();
        } catch (IOException e) {
            return "unreachable";
        }
    }
}
