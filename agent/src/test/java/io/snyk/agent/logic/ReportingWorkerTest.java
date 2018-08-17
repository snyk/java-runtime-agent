package io.snyk.agent.logic;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import io.snyk.agent.util.Log;
import io.snyk.agent.util.UseCounter;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportingWorkerTest {

    public static final Config NULL_CONFIG = new Config(null, Collections.emptyList(), null);

    JsonElement toJson(Consumer<UseCounter.Drain> drainer) {
        final UseCounter.Drain drain = new UseCounter.Drain();
        drainer.accept(drain);
        final String json = new ReportingWorker(new Log(),
                NULL_CONFIG,
                new ClassSource(new Log())).serialiseState(drain);

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
        toJson(drain -> drain.methodEntries.add("foo"));
        toJson(drain -> {
            drain.methodEntries.add("foo");
            drain.methodEntries.add("bar");
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
