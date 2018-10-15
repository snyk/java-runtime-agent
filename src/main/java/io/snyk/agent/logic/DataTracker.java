package io.snyk.agent.logic;

import io.snyk.agent.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataTracker {
    public final ClassInfo classInfo;

    final List<ObservedError> errors = Collections.synchronizedList(new ArrayList<>());

    public DataTracker(Log log) {
        this.classInfo = new ClassInfo(log, this::addError);
    }

    public void addError(String msg, Throwable e) {
        errors.add(new ObservedError(msg, e));
    }
}
