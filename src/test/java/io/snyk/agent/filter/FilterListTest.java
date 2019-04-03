package io.snyk.agent.filter;

import com.google.common.collect.ImmutableSet;
import io.snyk.agent.testutil.TestLogger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FilterListTest {
    @Test
    void loadBuiltInFilters() {
        final FilterList loaded = FilterList.loadBuiltInBolos(new TestLogger());
        assertNotEquals(0, loaded.numberOfClasses());

        // filter.provided-69.artifact = maven:org.apache.struts:struts2-core
        // filter.provided-69.version = [2.3.0,2.3.32)
        assertEquals(ImmutableSet.of("buildErrorMessage"), loaded.methodsToInstrumentInClass(
                "org/apache/struts2/dispatcher/multipart/JakartaMultiPartRequest",
                ImmutableSet.of()),
                "[data dependent] struts in unknown library");

        assertEquals(ImmutableSet.of("buildErrorMessage"), loaded.methodsToInstrumentInClass(
                "org/apache/struts2/dispatcher/multipart/JakartaMultiPartRequest",
                ImmutableSet.of("maven:org.apache.struts:struts2-core:2.3.7")),
                "[data dependent] struts in known bad library");

        assertEquals(ImmutableSet.of(), loaded.methodsToInstrumentInClass(
                "org/apache/struts2/dispatcher/multipart/JakartaMultiPartRequest",
                ImmutableSet.of("maven:org.apache.struts:struts2-core:2.3.40")),
                "[data dependent] struts in known not-bad library");
    }
}
