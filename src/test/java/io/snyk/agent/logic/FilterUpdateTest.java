package io.snyk.agent.logic;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.snyk.agent.filter.Filter;
import io.snyk.agent.testutil.TestLogger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterUpdateTest {

    @Test
    void ifModified() throws Exception {
        final WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();

        final Instrumentation ins = Mockito.mock(Instrumentation.class);
        AtomicReference<List<Filter>> filters = new AtomicReference<>(Collections.emptyList());

        final String TEST_URL = "/test/get-java";

        wireMock.stubFor(get(urlEqualTo(TEST_URL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Last-Modified", "Wed, 21 Oct 2015 07:28:00 GMT")
                        .withBody("filter.test-1.paths = foo/bar#quux")));

        wireMock.stubFor(get(urlEqualTo(TEST_URL))
            .withHeader("If-Modified-Since", equalTo("Wed, 21 Oct 2015 07:28:00 GMT"))
                .willReturn(aResponse()
                        .withStatus(304) // not-modified
                        .withBody("")));


        assertEquals(Collections.emptyList(), filters.get());

        final FilterUpdate filterUpdate = new FilterUpdate(
                new TestLogger(),
                ins,
                () -> {
                },
                new URL("http://localhost:" + wireMock.port() + TEST_URL),
                Instant.EPOCH,
                filters,
                1);

        assertTrue(filterUpdate.fetchUpdatedAnything(), "server has newer file, so we update");
        assertEquals(1, filters.get().size(), "update picked up the new rule");
        assertFalse(filterUpdate.fetchUpdatedAnything(), "server has the same file, so we don't update");
        assertEquals(1, filters.get().size(), "same rules as before");

        wireMock.stop();
    }
}