package io.snyk.agent.filter;

import io.snyk.agent.util.org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import io.snyk.agent.util.org.apache.maven.artifact.versioning.VersionRange;

import java.util.List;
import java.util.Map;

class VersionMatch {
    static boolean check(Map<String, VersionRange> wants, List<GroupArtifactVersion> haves) {
        for (GroupArtifactVersion have : haves) {
            final VersionRange versions = wants.get(have.groupArtifact);
            if (null == versions) {
                continue;
            }

            if (!(versions.containsVersion(new DefaultArtifactVersion(have.version)))) {
                return false;
            }

        }

        return true;
    }
}
