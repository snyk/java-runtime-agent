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
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
        configs.add("filter.foo.paths=**");
        final ReportingWorker reportingWorker = new ReportingWorker(new TestLogger(),
                Config.fromLinesWithoutDefault(configs.toArray(new String[0])),
                dataTracker,
                poster);
        reportingWorker.sendIfNecessary(() -> drain);

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

    @Test
    void supplierNotUsed() throws IOException {
        final AtomicLong used = new AtomicLong();
        final Supplier<UseCounter.Drain> supplier = () -> {
            used.incrementAndGet();
            return new UseCounter.Drain();
        };

        final ReportingWorker.Poster poster = (_prefix, _message) -> {};

        final TestLogger log = new TestLogger();
        final ReportingWorker worker = new ReportingWorker(log, Config.fromLinesWithoutDefault(
                "projectId=0153525f-5a99-4efe-a84f-454f12494033",
                "filter.foo.paths = **",
                "homeBaseUrl = invalid://url"
        ), new DataTracker(log), poster);

        worker.sendIfNecessary(supplier);
        assertEquals(1, used.longValue(), "should have read the supplier, to send");

        worker.sendIfNecessary(supplier);
        assertEquals(1, used.longValue(), "shouldn't have read the supplier again, last send was a success");

        // note: technically there's a race condition here; the second call must happen before the "events" timeout
        // ..but currently the timeout is >1 minute, and the test takes <1ms
    }

    public static void main(String[] args) throws Exception {
        final TestLogger log = new TestLogger();
        final DataTracker tracker = new DataTracker(log);
        final ReportingWorker worker = new ReportingWorker(log, Config.fromLinesWithoutDefault(
                "projectId=0153525f-5a99-4efe-a84f-454f12494033",
                "homeBaseUrl=http://localhost:8001/api/v1/beacon",
                "skipMetaPosts=true",
                "reportIntervalMs=0"
        ), tracker);

        final int values = 4_096;
        final Set<String> fakeEntries = new HashSet<>(values);
        for (int i = 0; i < values; i++) {
            fakeEntries.add(String.format(
                    "foooooooooooooooooooooooooooooooo%1$s:baaaaaaaaaaaaaaaaaaaaaaaaaaaaar%1$s:baaaaaaaaaaaaaaaaaaaaaaaaz%1$s:quuuuuuuuuuuuuuuuuuuux%1$s",
                    i));
        }

        final ExecutorService ex = Executors.newCachedThreadPool();

        final long min_time_ms = 250;

        final Callable<Object> postForever = () -> {
            while (true) {
                final long start = System.currentTimeMillis();
                final UseCounter.Drain drain = new UseCounter.Drain();
                drain.methodEntries.addAll(fakeEntries);
                worker.sendIfNecessary(() -> drain);

                final long duration = System.currentTimeMillis() - start;

                System.out.println("request took " + duration + "ms");

                if (duration < min_time_ms) {
                    Thread.sleep(min_time_ms - duration);
                }
            }
        };
        for (int thread = 0; thread < 4; thread++) {
            ex.submit(postForever);
        }
        ex.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }
}
