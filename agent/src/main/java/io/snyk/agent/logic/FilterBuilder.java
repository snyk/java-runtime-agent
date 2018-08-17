package io.snyk.agent.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterBuilder {
    private final String name;
    String artifact;
    String version;
    List<String> paths = new ArrayList<>();

    public FilterBuilder(String name) {
        this.name = name;
    }

    public void addPathsFrom(String value) {
        paths.addAll(Arrays.asList(value.split("\\s+")));
    }
}

