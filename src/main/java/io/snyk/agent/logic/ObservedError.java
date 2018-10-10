package io.snyk.agent.logic;

import io.snyk.agent.util.Throwables;

class ObservedError {
    public final String msg;
    public final String problem;

    ObservedError(String msg, Throwable problem) {
        this.msg = msg;
        this.problem = Throwables.getStackTrace(problem);
    }
}
