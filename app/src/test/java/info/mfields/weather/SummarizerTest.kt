package info.mfields.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class SummarizerTest {
    @Test fun groupsDailyHighLowAndSummary() {
        val hours = listOf(
            HourlyPeriod("2026-05-20T01:00:00-04:00", 60, "Clear", false),
            HourlyPeriod("2026-05-20T12:00:00-04:00", 74, "Sunny", true),
            HourlyPeriod("2026-05-20T18:00:00-04:00", 70, "Sunny", true),
            HourlyPeriod("2026-05-21T01:00:00-04:00", 58, "Cloudy", false),
            HourlyPeriod("2026-05-21T13:00:00-04:00", 68, "Cloudy", true)
        )
        val rows = summarize(hours)
        assertEquals(2, rows.size)
        assertEquals(74, rows[0].high)
        assertEquals(60, rows[0].low)
        assertEquals("Sunny", rows[0].summary)
    }
}
