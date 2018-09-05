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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportingWorkerTest {

    private JsonElement onlyDrain(Consumer<UseCounter.Drain> drainer) {
        return toJson(drainer, _jarInfoMap -> {
        }, Collections.emptyList());
    }

    private JsonElement onlyJar(Consumer<ClassSource> jarInfoAdder) {
        return toJson(_drainer -> {
        }, jarInfoAdder, Collections.emptyList());
    }

    private JsonElement onlyConfig(Collection<String> configLines) {
        return toJson(_drainer -> {
        }, _jarInfoAdder -> {
        }, configLines);
    }

    private JsonElement toJson(Consumer<UseCounter.Drain> drainer,
                               Consumer<ClassSource> jarInfoAdder,
                               Collection<String> configLines) {
        final UseCounter.Drain drain = new UseCounter.Drain();
        drainer.accept(drain);
        final ClassSource classSource = new ClassSource(new Log());
        jarInfoAdder.accept(classSource);

        final String json = new ReportingWorker(new Log(),
                Config.fromLines(configLines),
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
        onlyDrain(drain -> drain.methodEntries.add("foo:bar:baz:quux"));
        onlyDrain(drain -> {
            drain.methodEntries.add("foo:bar:baz:quux");
            drain.methodEntries.add("bar:bar:baz:quux");
        });

        onlyDrain(drain -> drain.loadClasses.put("foo", Sets.newHashSet()));
        onlyDrain(drain -> {
            drain.loadClasses.put("foo", Sets.newHashSet());
            drain.loadClasses.put("bar", Sets.newHashSet());
        });

        onlyDrain(drain -> {
            drain.loadClasses.put("foo", Sets.newHashSet("foo"));
        });
        onlyDrain(drain -> {
            drain.loadClasses.put("foo", Sets.newHashSet("foo", "bar"));
        });
        onlyJar(classSource -> {
            classSource.addError("foo", new Exception());
        });
        onlyJar(classSource -> {
            classSource.addError("foo", new Exception());
            classSource.addError("bar", new Exception());
        });
        onlyJar(classSource -> {
            classSource.jarInfoMap.put(URI.create("file://tmp/whatever"), Sets.newHashSet());
        });
        onlyJar(classSource -> {
            classSource.jarInfoMap.put(URI.create("file://tmp/whatever"),
                    Sets.newHashSet("maven:foo.bar:baz:12"));
        });
        onlyJar(classSource -> {
            classSource.jarInfoMap.put(URI.create("file://tmp/whatever"),
                    Sets.newHashSet("maven:foo.bar:baz:7.17", "maven:ooh.aah:baby:3.1.1"));
        });
        onlyJar(classSource -> {
            classSource.jarInfoMap.put(URI.create("file://tmp/whatever"),
                    Sets.newHashSet("maven:foo.bar:baz:12"));
            classSource.jarInfoMap.put(URI.create("file://tmp/other"),
                    Sets.newHashSet("maven:foo.bar:quux:13"));
        });

        onlyConfig(Collections.emptyList());
        onlyConfig(Arrays.asList(
                "filter.jakarta-multipart.artifact = maven:org.apache.struts:struts2-core",
                "filter.jakarta-multipart.paths = org/apache/struts2/dispatcher/multipart/JakartaMultiPartRequest#buildErrorMessage"
        ));

        onlyConfig(Arrays.asList(
                "filter.foo.paths = foo/**",
                "filter.bar.paths = bar/**#baz",
                "filter.bar.version = <1.3.3",
                "filter.bar.artifact = maven:foo:bar"
        ));
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
