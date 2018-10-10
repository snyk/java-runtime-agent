package io.snyk.agent.api;

import java.time.Instant;

public class MethodEntry {
    public final Instant when;
    public final String filter;
    public final String className;
    public final String methodName;

    public MethodEntry(String filter, String className, String methodName) {
        this.when = Instant.now();
        this.filter = filter;
        this.className = className;
        this.methodName = methodName;
    }
}
