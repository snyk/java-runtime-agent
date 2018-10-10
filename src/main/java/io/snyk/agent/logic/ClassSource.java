package io.snyk.agent.logic;

import io.snyk.agent.util.Log;

import java.net.URI;
import java.util.*;

public class ClassSource {
    final ClassInfo classInfo;

    final List<ObservedError> errors = Collections.synchronizedList(new ArrayList<>());
    private final Log log;

    public ClassSource(Log log) {
        this.log = log;
        this.classInfo = new ClassInfo(log);
    }

    public ClassInfo.ExtraInfo findSourceInfo(final ClassLoader loader,
                                              final String className,
                                              final byte[] classfileBuffer) {
        return classInfo.findSourceInfo(loader, className, classfileBuffer);
    }

    public void addError(String msg, Throwable e) {
        errors.add(new ObservedError(msg, e));
    }

    Set<String> infoFor(URI sourceUri) {
        return classInfo.infoFor(sourceUri);
    }

    Map<URI, Set<String>> all() {
        return classInfo.all();
    }
}
