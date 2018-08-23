package io.snyk.agent.logic;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import io.snyk.agent.util.Log;
import io.snyk.agent.util.UseCounter;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportingWorkerTest {

    public static final Config NULL_CONFIG = new Config(null, Collections.emptyList(), null);

    JsonElement toJson(Consumer<UseCounter.Drain> drainer) {
        return toJson(drainer, _jarInfoMap -> {
        });
    }

    JsonElement toJson(Consumer<UseCounter.Drain> drainer,
                       Consumer<ConcurrentMap<URI, Set<String>>> jarInfoAdder) {
        final UseCounter.Drain drain = new UseCounter.Drain();
        drainer.accept(drain);
        final ClassSource classSource = new ClassSource(new Log());
        jarInfoAdder.accept(classSource.jarInfoMap);

        final String json = new ReportingWorker(new Log(),
                NULL_CONFIG,
                classSource).serialiseState(drain);

        System.err.println(json);

        // this weird dance is important; half of the methods turn leniency back on for you,
        // and we really care
        final JsonReader parser = new JsonReader(new StringReader(json));
        parser.setLenient(false);
        return Streams.parse(parser);
    }

    @Test
    void serialiseWeirdListSizesCorrectly() {
        toJson(drain -> {
        });
        toJson(drain -> drain.methodEntries.add("foo:bar:baz:quux"));
        toJson(drain -> {
            drain.methodEntries.add("foo:bar:baz:quux");
            drain.methodEntries.add("bar:bar:baz:quux");
        });

        toJson(drain -> drain.loadClasses.put("foo", Sets.newHashSet()));
        toJson(drain -> {
            drain.loadClasses.put("foo", Sets.newHashSet());
            drain.loadClasses.put("bar", Sets.newHashSet());
        });

        toJson(drain -> {
            drain.loadClasses.put("foo", Sets.newHashSet("foo"));
        });
        toJson(drain -> {
            drain.loadClasses.put("foo", Sets.newHashSet("foo", "bar"));
        });
        toJson(drain -> {
                },
                jarInfoMap -> {
                    jarInfoMap.put(URI.create("file://tmp/whatever"), Sets.newHashSet());
                });
        toJson(drain -> {
                },
                jarInfoMap -> {
                    jarInfoMap.put(URI.create("file://tmp/whatever"), Sets.newHashSet("maven:foo.bar:baz:12"));
                });
        toJson(drain -> {
                },
                jarInfoMap -> {
                    jarInfoMap.put(URI.create("file://tmp/whatever"),
                            Sets.newHashSet("maven:foo.bar:baz:7.17", "maven:ooh.aah:baby:3.1.1"));
                });
        toJson(drain -> {
                },
                jarInfoMap -> {
                    jarInfoMap.put(URI.create("file://tmp/whatever"), Sets.newHashSet("maven:foo.bar:baz:12"));
                    jarInfoMap.put(URI.create("file://tmp/other"), Sets.newHashSet("maven:foo.bar:quux:13"));
                });
    }

    @Test
    void popTrailingComma() {
        final StringBuilder sb = new StringBuilder("hello,");
        ReportingWorker.trimRightCommaSpacing(sb);
        assertEquals("hello", sb.toString());
    }

    @Test
    void popTrailingSpacesComma() {
        final StringBuilder sb = new StringBuilder("hello  , ");
        ReportingWorker.trimRightCommaSpacing(sb);
        assertEquals("hello", sb.toString());
    }

    @Test
    void popTrailingCommaAbsent() {
        final StringBuilder sb = new StringBuilder("hello");
        ReportingWorker.trimRightCommaSpacing(sb);
        assertEquals("hello", sb.toString());
    }
}
