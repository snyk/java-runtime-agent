package io.snyk.agent.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathFilterTest {

    @Test
    public void testMatching() {
        final PathFilter fooBarExplicit = PathFilter.parse("io.snyk.Foo#bar");
        assertTrue(fooBarExplicit.test("io.snyk.Foo#bar"));
        assertFalse(fooBarExplicit.test("io.snyk.quux.Foo#bar"));

        final PathFilter fooExplicit = PathFilter.parse("io.snyk.Foo");

        assertTrue(fooExplicit.test("io.snyk.Foo#bar"));
        assertTrue(fooExplicit.test("io.snyk.Foo#baz"));

        final PathFilter fooBarWild = PathFilter.parse("io.snyk.**#fooBar");
        assertTrue(fooBarWild.test("io.snyk.foo.Agent#fooBar"));
        assertTrue(fooBarWild.test("io.snyk.foo.Agent#fooBar()"));
        assertTrue(fooBarWild.test("io.snyk.foo.Agent#fooBar(Z)"));

        assertTrue(fooBarWild.test("io.snyk.Agent#fooBar"));
        // invalid, but accepted
        assertTrue(fooBarWild.test("io.snyk.#fooBar"));
        assertTrue(fooBarWild.test("io.snyk.Agent#fooBar(nonsense here)"));

        assertFalse(fooBarWild.test("nio.snyk.Agent#fooBar"));
        assertFalse(fooBarWild.test("io.snykn.Agent#fooBar"));
        assertFalse(fooBarWild.test("io.snyk.Agent#fooBa"));
        assertFalse(fooBarWild.test("io.snyk.Agent#ooBar"));
        assertFalse(fooBarWild.test("io.snyk.Agent#nfooBar"));
        assertFalse(fooBarWild.test("io.snyk.Agent#fooBarn"));
    }
}
