package io.snyk.agent.filter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.snyk.agent.util.org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.snyk.agent.filter.VersionMatch.check;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unchecked")
class VersionMatchTest {
    @Test
    public void singleMatching() {
        assertTrue(check(
                gaRange("foo:bar", "[1,2)"),
                list(gav("foo:bar", "1.5"))),
                "identified artifact, version in range");
        assertFalse(check(
                gaRange("foo:bar", "[1,2)"),
                list(gav("foo:bar", "0.5"))),
                "identified artifact, version too low");
        assertFalse(check(
                gaRange("foo:bar", "[1,2)"),
                list(gav("foo:bar", "2.5"))),
                "identified artifact, version too high");

        assertTrue(check(
                gaRange("foo:bar", "[1,2),[4,5)"),
                list(gav("foo:bar", "1.5"))),
                "identified artifact, version in first range");
        assertTrue(check(
                gaRange("foo:bar", "[1,2),[4,5)"),
                list(gav("foo:bar", "4.5"))),
                "identified artifact, version in second range");
        assertFalse(check(
                gaRange("foo:bar", "[1,2)"),
                list(gav("foo:bar", "0.5"))),
                "identified artifact, version between ranges");
    }

    @Test
    public void multipleRange() {
        assertTrue(check(
                maps(),
                list(gav("foo:bar", "1.5"))),
                "no wanted (illegal)");

        assertTrue(check(
                maps(
                        gaRange("foo:bar", "[1,2)"),
                        gaRange("egg:pat", "[7,8)"),
                        gaRange("leg:hat", "[6,7)")),
                list(gav("foo:bar", "1.5"))),
                "one of many artifacts");
    }

    @Test
    public void multipleVersions() {
        assertTrue(check(
                maps(),
                list(
                        gav("foo:bar", "1.5"),
                        gav("egg:pat", "1.5")

                )),
                "no wanted");

        assertTrue(check(
                gaRange("foo:bar", "[1,2)"),
                list(
                        gav("foo:bar", "1.5"),
                        gav("egg:pat", "1.5"),
                        gav("leg:hat", "1.5"))),
                "one of many artifacts, first");

        assertTrue(check(
                gaRange("foo:bar", "[1,2)"),
                list(
                        gav("egg:pat", "1.5"),
                        gav("leg:hat", "1.5"),
                        gav("foo:bar", "1.5"))),
                "one of many artifacts, last");

        assertTrue(check(
                gaRange("foo:bar", "[1,2)"),
                list(
                        gav("egg:pat", "1.5"),
                        gav("leg:hat", "1.5"))),
                "multiple non-matching");
    }

    static <K, V> Map<K, V> maps(Map<K, V>... maps) {
        Map<K, V> ret = new HashMap<>();
        for (Map<K, V> map : maps) {
            ret.putAll(map);
        }
        return ret;
    }

    static Map<String, VersionRange> gaRange(String groupArtifact, String versionRanges) {
        return ImmutableMap.of(groupArtifact, VersionRange.createFromVersionSpec(versionRanges));
    }

    static GroupArtifactVersion gav(String groupArtifact, String version) {
        return new GroupArtifactVersion(groupArtifact, version);
    }

    static <T> List<T> list(T... items) {
        return ImmutableList.copyOf(items);
    }
}
