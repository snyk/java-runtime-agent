package io.snyk.agent.output;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GavKey {
    final List<String> gavs;

    public GavKey(List<String> gavs) {
        final List<String> unsorted = new ArrayList<>(gavs);
        Collections.sort(unsorted);
        this.gavs = Collections.unmodifiableList(unsorted);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GavKey gavKey = (GavKey) o;
        return gavs.equals(gavKey.gavs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gavs);
    }
}
