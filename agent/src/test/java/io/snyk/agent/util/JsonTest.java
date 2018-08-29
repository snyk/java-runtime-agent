package io.snyk.agent.util;

import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonTest {

    private static String stringy(String what) {
        final StringBuilder sb = new StringBuilder();
        Json.appendString(sb, what);
        return sb.toString();
    }

    @Test
    void escapeString() {
        assertEquals("\"hello\"", stringy("hello"));
        assertEquals("\"hello\\nworld\"", stringy("hello\nworld"));
        assertEquals("\"hello\\\"world\"", stringy("hello\"world"));
        assertEquals("\"hello\\\\world\"", stringy("hello\\world"));
    }

    @Test
    void escapeStringAgainst() {
        for (String example : new String[]{
                "Tokyo is named 東京都",
                "emoji \uD83E\uDD1E\uD83C\uDFFD fingers",
        }) {
            assertEquals("\"" + StringEscapeUtils.escapeJson(example) + "\"", stringy(example));
        }
    }
}
