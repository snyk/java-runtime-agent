package io.snyk.agent.output;

import io.snyk.agent.util.Throwables;

public class ObservedError {
    public final String msg;
    public final String problem;

    public ObservedError(String msg, Throwable problem) {
        this.msg = msg;
        this.problem = Throwables.getStackTrace(problem);
    }
}
