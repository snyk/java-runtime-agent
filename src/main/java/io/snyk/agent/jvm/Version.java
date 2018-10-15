package io.snyk.agent.jvm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * load version.txt from the archive
 */
class Version {
    static String extendedVersionInfo() {
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(Version.class.getResourceAsStream(
                "/version.txt")))) {
            return br.readLine();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}