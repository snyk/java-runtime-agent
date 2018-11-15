package io.snyk.agent.logic;

public enum LogDestinationConfig {
    FILE,
    STDERR,
    NOWHERE;

    static LogDestinationConfig fromNullableString(String input) {
        if (null == input) {
            return FILE;
        }
        switch (input) {
            case "file":
                return FILE;
            case "stderr":
                return STDERR;
            case "nowhere":
                return NOWHERE;
            default:
                throw new IllegalStateException("unrecognised log destination: " + input);
        }
    }
}
