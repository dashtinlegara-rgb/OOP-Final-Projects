package com.legara.campusconnect.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConcurrencyHazardDemoTest {

    @Test
    @DisplayName("H01 Shared counter: the synchronized version never loses updates")
    void synchronizedCounterIsExact() throws Exception {
        assertEquals(4 * 200_000, ConcurrencyHazardDemo.countSynchronized(4, 200_000));
    }
}
