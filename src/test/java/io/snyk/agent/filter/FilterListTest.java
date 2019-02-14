package io.snyk.agent.filter;

import io.snyk.agent.testutil.TestLogger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FilterListTest {
    @Test
    void loadBuiltInFilters() {
        assertNotEquals(0, FilterList.loadBuiltInBolos(new TestLogger()).numberOfClasses());
    }
}
