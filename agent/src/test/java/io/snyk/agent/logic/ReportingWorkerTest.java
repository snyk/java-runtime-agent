package io.snyk.agent.logic;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.stream.JsonReader;
import io.snyk.agent.util.UseCounter;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ReportingWorkerTest {

    JsonElement toJson(Consumer<UseCounter.Drain> drainer) {
        final UseCounter.Drain drain = new UseCounter.Drain();
        drainer.accept(drain);
        final String json = new ReportingWorker().serialiseState(drain, "project-id");

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
        ReportingWorker.popTrailingCommaIfPresent(sb);
        assertEquals("hello", sb.toString());
    }

    @Test
    void popTrailingCommaAbsent() {
        final StringBuilder sb = new StringBuilder("hello");
        ReportingWorker.popTrailingCommaIfPresent(sb);
        assertEquals("hello", sb.toString());
    }
}
