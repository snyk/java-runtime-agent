package io.snyk.agent.jvm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionTest {
    @Test
    public void testVersionExists() {
        assertTrue(Version.extendedVersionInfo().startsWith("git:"));
    }
}