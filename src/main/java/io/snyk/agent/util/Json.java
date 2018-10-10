package io.snyk.agent.util;

/**
 * Remember, we can't use any standard library because we get loaded at the wrong time.
 * This is not supposed to be a full JSON implementation. It's the smallest possible thing
 * which could work.
 */
public class Json {
    public static void appendString(StringBuilder into, CharSequence source) {
        into.append("\"");
        source.codePoints().forEachOrdered(cp -> {
            switch (cp) {
                case '"':
                    into.append("\\\"");
                    return;
                case '\\':
                    into.append("\\\\");
                    return;
                case '\n':
                    into.append("\\n");
                    return;
                case '\t':
                    into.append("\\t");
                    return;
                case '\r':
                    into.append("\\r");
                    return;
            }
            if (cp >= 0x20 && cp < 0x7f) {
                // other character in the printable range
                into.appendCodePoint(cp);
            } else if (Character.isSupplementaryCodePoint(cp)) {
                into.append(slashUEscaped(Character.highSurrogate(cp)));
                into.append(slashUEscaped(Character.lowSurrogate(cp)));
            } else {
                into.append(slashUEscaped(cp));
            }
        });
        into.append("\"");
    }

    private static String slashUEscaped(int cp) {
        return String.format("\\u%04X", cp);
    }
}
