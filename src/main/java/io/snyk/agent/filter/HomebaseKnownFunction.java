package io.snyk.agent.filter;

import java.util.Objects;

public class HomebaseKnownFunction {
    public final String artifact;
    public final String className;
    public final String methodName;

    public HomebaseKnownFunction(String artifact, String className, String methodName) {
        this.artifact = artifact;
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HomebaseKnownFunction that = (HomebaseKnownFunction) o;
        return artifact.equals(that.artifact) &&
                className.equals(that.className) &&
                methodName.equals(that.methodName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact, className, methodName);
    }
}
