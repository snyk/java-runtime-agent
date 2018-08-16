package io.snyk.agent.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Filter {
    private final String name;
    public String artifact;
    public String version;
    public List<String> paths = new ArrayList<>();

    public Filter(String name) {
        this.name = name;
    }

    public void addPathsFrom(String value) {
        paths.addAll(Arrays.asList(value.split("\\s+")));
    }
}

