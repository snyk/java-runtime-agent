package io.snyk.agent.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Crc32cTest {

    @Test
    void process() {
        // this is a public test vector, and the high bits come out okay
        assertEquals(0xe3069283, Crc32c.process("123456789".getBytes()));
    }
}
