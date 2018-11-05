package io.snyk.agent.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathFilterTest {

    @Test
    void testMatching() {
        final PathFilter fooBarExplicit = PathFilter.parse("io.snyk.Foo#bar");
        assertTrue(fooBarExplicit.testMethod("io.snyk.Foo", "bar"));
        assertFalse(fooBarExplicit.testMethod("io.snyk.quux.Foo", "bar"));

        final PathFilter fooExplicit = PathFilter.parse("io.snyk.Foo");

        assertTrue(fooExplicit.testMethod("io.snyk.Foo", "bar"));
        assertTrue(fooExplicit.testMethod("io.snyk.Foo", "baz"));

        final PathFilter fooBarWild = PathFilter.parse("io.snyk.**#fooBar");
        assertTrue(fooBarWild.testMethod("io.snyk.foo.Agent", "fooBar"));

        assertTrue(fooBarWild.testMethod("io.snyk.Agent", "fooBar"));
        // invalid, but accepted
        assertTrue(fooBarWild.testMethod("io.snyk.", "fooBar"));

        assertFalse(fooBarWild.testMethod("nio.snyk.Agent", "fooBar"));
        assertFalse(fooBarWild.testMethod("io.snykn.Agent", "fooBar"));
        assertFalse(fooBarWild.testMethod("io.snyk.Agent", "fooBa"));
        assertFalse(fooBarWild.testMethod("io.snyk.Agent", "ooBar"));
        assertFalse(fooBarWild.testMethod("io.snyk.Agent", "nfooBar"));
        assertFalse(fooBarWild.testMethod("io.snyk.Agent", "fooBarn"));
    }
}
