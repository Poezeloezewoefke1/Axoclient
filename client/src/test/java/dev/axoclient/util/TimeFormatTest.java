package dev.axoclient.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TimeFormatTest {

    @Test
    void underAnHour() {
        assertEquals("0:00", TimeFormat.clock(0));
        assertEquals("1:05", TimeFormat.clock(65_000));
    }

    @Test
    void pastAnHour() {
        assertEquals("1:02:05", TimeFormat.clock(3_725_000));
    }

    @Test
    void negativeIsZero() {
        assertEquals("0:00", TimeFormat.clock(-5));
    }

    @Test
    void stopwatchTenths() {
        assertEquals("1:05.3", TimeFormat.stopwatch(65_399));
        assertEquals("0:00.0", TimeFormat.stopwatch(0));
    }
}
