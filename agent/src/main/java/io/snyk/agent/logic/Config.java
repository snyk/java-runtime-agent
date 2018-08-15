package io.snyk.agent.logic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Config {
    public final String projectId;
    public final String filter;
    public final String urlPrefix;

    private Config(String projectId, String filter, String urlPrefix) {
        this.projectId = null != projectId ? projectId : "no-project-id-provided";
        this.filter = filter;
        this.urlPrefix = null != urlPrefix ? urlPrefix : "http://localhost:8000";
    }

    public static Config fromFile(String path) {
        try {
            return fromLines(Files.readAllLines(new File(path).toPath()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Config fromLines(Iterable<String> lines) {
        String projectId = null;
        String filter = null;
        String urlPrefix = null;

        // this looks awfully like a .properties file. Maybe it could be a .properties file?
        // .properties is awful at unicode and multi-value, but we probably don't care

        for (String line : lines) {
            if (line.startsWith("#")) {
                continue;
            }

            final String stripped = line.trim();

            if (stripped.isEmpty()) {
                continue;
            }

            final String[] splitUp = stripped.split("\\s*=\\s*", 2);
            final String key = splitUp[0];
            final String value = splitUp[1];

            switch (key) {
                case "projectId":
                    projectId = value;
                    break;
                case "filter":
                    filter = value;
                    break;
                case "urlPrefix":
                    urlPrefix = value;
                    break;
                default:
                    System.err.println("snyk-agent: unrecognised key: " + key);
            }
        }

        return new Config(projectId, filter, urlPrefix);
    }
}
