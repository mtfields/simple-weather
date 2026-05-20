package info.mfields.weather

import info.mfields.weather.domain.Summarizer
import info.mfields.weather.network.HourlyPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class SummarizerTest {
    @Test fun summarizesByDay() {
        val periods = listOf(
            HourlyPeriod("2026-05-20T01:00:00-04:00", 60, "Cloudy", false),
            HourlyPeriod("2026-05-20T14:00:00-04:00", 75, "Sunny", true),
            HourlyPeriod("2026-05-21T03:00:00-04:00", 55, "Rain", false),
            HourlyPeriod("2026-05-21T12:00:00-04:00", 70, "Rain", true)
        )
        val out = Summarizer.daily(periods)
        assertEquals(2, out.size)
        assertEquals(75, out[0].high)
        assertEquals(60, out[0].low)
    }
}
