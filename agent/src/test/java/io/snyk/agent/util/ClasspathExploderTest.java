package io.snyk.agent.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClasspathExploderTest {

    @Test
    void walkClassPath() {
        new ClasspathExploder().walkClassPath();
    }
}