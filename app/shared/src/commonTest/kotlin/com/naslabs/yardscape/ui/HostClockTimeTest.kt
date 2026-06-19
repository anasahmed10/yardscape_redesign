package com.naslabs.yardscape.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HostClockTimeTest {
    @Test
    fun epochMillisFormatsAsHostClockTime() {
        assertEquals("12:00 AM", 0L.toHostClockTimeLabel())
        assertEquals("9:05 AM", (9L * MILLIS_PER_HOUR + 5L * MILLIS_PER_MINUTE).toHostClockTimeLabel())
        assertEquals("12:30 PM", (12L * MILLIS_PER_HOUR + 30L * MILLIS_PER_MINUTE).toHostClockTimeLabel())
        assertEquals("11:45 PM", (23L * MILLIS_PER_HOUR + 45L * MILLIS_PER_MINUTE).toHostClockTimeLabel())
    }

    @Test
    fun clockInputAcceptsAmPmAndTwentyFourHourTimes() {
        assertEquals(0, "12:00 AM".toClockMinutesSinceMidnight())
        assertEquals(9 * 60, "9 AM".toClockMinutesSinceMidnight())
        assertEquals(21 * 60 + 15, "9:15 pm".toClockMinutesSinceMidnight())
        assertEquals(14 * 60 + 30, "14:30".toClockMinutesSinceMidnight())
    }

    @Test
    fun invalidClockInputIsRejected() {
        assertNull("".toClockMinutesSinceMidnight())
        assertNull("25:00".toClockMinutesSinceMidnight())
        assertNull("9:99 AM".toClockMinutesSinceMidnight())
        assertNull("0 PM".toClockMinutesSinceMidnight())
    }

    @Test
    fun clockInputUpdatesTimeWithoutChangingEventDay() {
        val dayThreeAtSixAm = 3L * MILLIS_PER_DAY + 6L * MILLIS_PER_HOUR

        assertEquals(
            3L * MILLIS_PER_DAY + 10L * MILLIS_PER_HOUR + 30L * MILLIS_PER_MINUTE,
            dayThreeAtSixAm.withHostClockTime("10:30 AM"),
        )
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60L * 1_000L
        const val MILLIS_PER_HOUR = 60L * MILLIS_PER_MINUTE
        const val MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR
    }
}
