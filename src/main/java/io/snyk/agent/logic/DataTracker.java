package io.snyk.agent.logic;

import io.snyk.agent.output.ObservedError;
import io.snyk.agent.output.ObservedWarning;
import io.snyk.agent.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The shared state between the instrumentation, and the reporting.
 * <p>
 * You'd think that the events themselves would be in here, but they're not:
 * they are generated statically (without a reference to a class), so they are
 * directly in the {@link io.snyk.agent.jvm.LandingZone}.
 */
public class DataTracker {
    public final ClassInfo classInfo;

    final List<ObservedWarning> warnings = Collections.synchronizedList(new ArrayList<>());

    final List<ObservedError> errors = Collections.synchronizedList(new ArrayList<>());

    public DataTracker(Log log) {
        this.classInfo = new ClassInfo(log, this::addError);
    }

    public void addWarning(String msg) {
        warnings.add(new ObservedWarning(msg));
    }

    public void addError(String msg, Throwable e) {
        errors.add(new ObservedError(msg, e));
    }
}
