package io.snyk.agent.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionFilterTest {

    @Test
    void testThree() {
        final VersionFilter underThree = VersionFilter.parse("[,3)");
        assertTrue(underThree.test("1"));
        assertTrue(underThree.test("2"));
        assertFalse(underThree.test("3"));
        assertFalse(underThree.test("4"));

        assertTrue(underThree.test("2.5"));
        assertFalse(underThree.test("3.5"));
    }

    @Test
    void testThreeOpen() {
        final VersionFilter underThree = VersionFilter.parse("[,3]");
        assertTrue(underThree.test("2"));
        assertTrue(underThree.test("3"));
        assertFalse(underThree.test("4"));
    }

    @Test
    void testMultiple() {
        final VersionFilter underThree = VersionFilter.parse("[1,2) [4,5)");
        assertTrue(underThree.test("1"));
        assertTrue(underThree.test("1.5"));
        assertFalse(underThree.test("2"));
        assertFalse(underThree.test("3"));
        assertTrue(underThree.test("4"));
        assertTrue(underThree.test("4.5"));
        assertFalse(underThree.test("5"));
    }
}
