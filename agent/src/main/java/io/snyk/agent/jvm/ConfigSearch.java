package io.snyk.agent.jvm;

import io.snyk.agent.util.Log;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

class ConfigSearch {

    private static final String AGENT_PROPS = "snyk-agent.properties";

    static File find(String agentArguments) {
        final String provided;
        if (null != agentArguments && !agentArguments.isEmpty()) {
            if (!agentArguments.startsWith("file:")) {
                Log.loading("invalid arguments, expected file:, not " + agentArguments);
                return null;
            }

            provided = agentArguments.substring("file:".length());

            final File candidate = new File(provided);
            if (candidate.isAbsolute()) {
                return candidate;
            }
        } else {
            provided = AGENT_PROPS;
        }

        Log.loading("resolving \"" + provided + "\" to an absolute path");

        for (File searchPath : searchIn()) {
            final File candidate = new File(searchPath, provided);
            Log.loading("trying: " + candidate.getAbsolutePath());
            if (candidate.isFile()) {
                return candidate;
            }
        }

        Log.loading("failed: file could not be found, please specify a valid absolute path");
        return null;
    }

    /**
     * A list of places we might find the config file, if the user didn't specify an absolute path on the command line.
     */
    private static List<File> searchIn() {
        final List<File> locations = new ArrayList<>();
        final File ourSourceLocation = ourSourceLocation();
        if (null != ourSourceLocation) {
            locations.add(ourSourceLocation);
        }

        // so.. current directory? Not going to make sense to most people.

        // Just one then.

        return locations;
    }

    /**
     * Try and resolve the path of the agent jar, using a simpler (less reliable) method than the main agent code uses.
     *
     * We could use the same code? We're just trying to help here.
     */
    private static File ourSourceLocation() {
        final ProtectionDomain pd;

        try {
            pd = ConfigSearch.class.getProtectionDomain();
        } catch (SecurityException ignored) {
            return null;
        }

        final CodeSource source = pd.getCodeSource();

        if (null == source) {
            return null;
        }

        final URL location = source.getLocation();

        if (null == location) {
            return null;
        }

        if (!"file".equals(location.getProtocol())) {
            return null;
        }

        return new File(location.getPath()).getParentFile();
    }
}
