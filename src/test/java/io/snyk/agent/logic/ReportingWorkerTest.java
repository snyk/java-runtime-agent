package io.snyk.agent.logic;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.snyk.agent.testutil.TestLogger;
import io.snyk.agent.util.UseCounter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReportingWorkerTest {

    // yes, these helpers have got totally out of control.
    // no, I don't have a solution
    private void onlyDrain(Consumer<UseCounter.Drain> drainer) throws IOException {
        toJson(drainer, _jarInfoMap -> {
        }, _errorInfoMap -> {
        }, Collections.emptyList());
    }

    private void onlyJar(Consumer<ClassInfo> jarInfoAdder) throws IOException {
        toJson(_drainer -> {
        }, jarInfoAdder, _errorInfoMap -> {
        }, Collections.emptyList());
    }

    private void onlyError(Consumer<DataTracker> errorInfoAdder) throws IOException {
        toJson(_drainer -> {
        }, _jarInfo -> {
        }, errorInfoAdder, Collections.emptyList());
    }

    private void onlyConfig(Collection<String> configLines) throws IOException {
        toJson(_drainer -> {
        }, _jarInfoAdder -> {
        }, _errorInfoMap -> {
        }, configLines);
    }

    private void toJson(Consumer<UseCounter.Drain> drainer,
                        Consumer<ClassInfo> jarInfoAdder,
                        Consumer<DataTracker> errorInfoAdder,
                        Collection<String> configLines) throws IOException {
        final UseCounter.Drain drain = new UseCounter.Drain();
        drainer.accept(drain);
        final DataTracker dataTracker = new DataTracker(new TestLogger());
        jarInfoAdder.accept(dataTracker.classInfo);
        errorInfoAdder.accept(dataTracker);

        final List<CharSequence> postings = new ArrayList<>();
        final ReportingWorker.Poster poster = (_prefix, message) -> postings.add(message);

        final List<String> configs = Lists.newArrayList(configLines);
        configs.add("projectId=1f9378b7-46fa-41ea-a156-98f7a8930ee1");
        final ReportingWorker reportingWorker = new ReportingWorker(new TestLogger(),
                Config.fromLinesWithoutDefault(configs),
                dataTracker);
        reportingWorker.doPosting(drain, poster);
        assertValidJson(new String(reportingWorker.buildFullMessage(
                reportingWorker.jsonHeader().toString(),
                "\"fragment\": true"),
                StandardCharsets.UTF_8));

        assertFalse(postings.isEmpty(), "at least the metadata should be sent");

        for (CharSequence fragment : postings) {
            final String json = "{" + fragment + "}";
            assertValidJson(json);
        }
    }

    private void assertValidJson(String json) throws IOException {
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
        onlyError(classSource -> {
            classSource.addError("foo", new Exception());
        });
        onlyError(classSource -> {
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
                "filter.bar.version = [,1.3.3)",
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
