package io.snyk.checkdb;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static java.time.format.DateTimeFormatter.ISO_DATE;

public class Walk {
    private static final Logger logger = LoggerFactory.getLogger(Walk.class);

    public static void main(String[] args)
            throws Exception {
        try (final MavenIndex index = new MavenIndex()) {
            index.maybeUpdateIndex();
            walk(index);
        }
    }


    private static void walk(MavenIndex index) throws IOException {
        // two years
        final long MAX_AGE = 1000L * 60 * 60 * 24 * 365 * 2;

        final Map<String, DatedVersion> latest = new HashMap<>(16 * 4096);

        index.forEach(ai -> {
            final long modified = ai.getLastModified();
            final ComparableVersion version = new ComparableVersion(ai.getVersion());

            final String key = ai.getGroupId() + ":" + ai.getArtifactId();

            latest.compute(key, (_key, old) -> {
                if (null == old) {
                    // no existing version, so use this
                    return new DatedVersion(modified, version);
                }

                // positive for this being newer
                final long timeDifference = modified - old.date;
                if (version.compareTo(old.version) > 0) {
                    if (timeDifference > -MAX_AGE) {
                        // we're newer, and not depressingly old
                        return new DatedVersion(modified, version);
                    } else {
                        // e.g. https://search.maven.org/search?q=g:org.glassfish.web%20AND%20a:webtier-all&core=gav
                        logger.warn("update too old: " + key + ": "
                                + old.version + " -> " + version + " in "
                                + toDate(old.date)
                                + " -> "
                                + toDate(modified));
                    }
                }

                return old;
            });
        });

        logger.info(latest.size() + " records");

        final List<String> strings = new ArrayList<>(latest.keySet());
        Collections.sort(strings);

        try (final PrintWriter pw = new PrintWriter(new FileOutputStream("artifacts.lst"))) {
            for (String gav : strings) {
                pw.println(gav + ":" + latest.get(gav).version);
            }
        }
    }

    static class DatedVersion {
        final long date;
        final ComparableVersion version;

        DatedVersion(long date, ComparableVersion version) {
            this.date = date;
            this.version = version;
        }
    }

    private static String toDate(long date) {
        // I really don't like these APIs.
        return ISO_DATE.format(Instant.ofEpochMilli(date).atZone(ZoneOffset.UTC));
    }
}
