package io.snyk.agent.filter;

public class GroupArtifactVersion {
    public final String groupArtifact;
    public final String version;

    public GroupArtifactVersion(String groupArtifact, String version) {
        this.groupArtifact = groupArtifact;
        this.version = version;
    }
}
