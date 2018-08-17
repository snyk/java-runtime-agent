package io.snyk.agent.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionFilterTest {

    @Test
    void testMatching() {
        final VersionFilter underThree = VersionFilter.parse("<3");
        assertTrue(underThree.test("1"));
        assertTrue(underThree.test("2"));
        assertFalse(underThree.test("3"));
        assertFalse(underThree.test("4"));

        assertTrue(underThree.test("2.5"));
        assertFalse(underThree.test("3.5"));
    }
}