package io.snyk.agent.logic;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.snyk.agent.util.Log;
import io.snyk.agent.util.UseCounter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class ReportingWorkerTest {

    private void onlyDrain(Consumer<UseCounter.Drain> drainer) throws IOException {
        toJson(drainer, _jarInfoMap -> {
        }, Collections.emptyList());
    }

    private void onlyJar(Consumer<ClassSource> jarInfoAdder) throws IOException {
        toJson(_drainer -> {
        }, jarInfoAdder, Collections.emptyList());
    }

    private void onlyConfig(Collection<String> configLines) throws IOException {
        toJson(_drainer -> {
        }, _jarInfoAdder -> {
        }, configLines);
    }

    private void toJson(Consumer<UseCounter.Drain> drainer,
                        Consumer<ClassSource> jarInfoAdder,
                        Collection<String> configLines) throws IOException {
        final UseCounter.Drain drain = new UseCounter.Drain();
        drainer.accept(drain);
        final ClassSource classSource = new ClassSource(new Log());
        jarInfoAdder.accept(classSource);

        final List<CharSequence> postings = new ArrayList<>();
        final ReportingWorker.Poster poster = postings::add;

        final List<String> configs = Lists.newArrayList(configLines);
        configs.add("projectId=1f9378b7-46fa-41ea-a156-98f7a8930ee1");
        new ReportingWorker(new Log(),
                Config.fromLines(configs),
                classSource).doPosting(drain, poster);

        assertFalse(postings.isEmpty(), "at least the metadata should be sent");

        for (CharSequence fragment : postings) {
            final String json = "{" + fragment + "}";
            // this weird dance is important; half of the methods turn leniency back on for you,
            // and we really care
            System.err.println("input: " + json);
            final JsonReader parser = new JsonReader(new StringReader(json));
            parser.setLenient(false);
            System.err.println("output: " + Streams.parse(parser));

            // TODO: we're not actually confirming there's anything useful in here, only that it's valid JSON

            // what an odd API
            assertEquals(JsonToken.END_DOCUMENT, parser.peek());
        }
    }

    @Test
    void serialiseWeirdListSizesCorrectly() throws IOException {
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
