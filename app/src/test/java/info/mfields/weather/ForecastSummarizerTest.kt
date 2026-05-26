package info.mfields.weather

import info.mfields.weather.network.ForecastSummarizer
import info.mfields.weather.network.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastSummarizerTest {
    @Test fun summarizesByDay() {
        val p = listOf(
            Period("2026-05-20T09:00:00-04:00", 70, "Sunny", true),
            Period("2026-05-20T21:00:00-04:00", 55, "Clear", false),
            Period("2026-05-21T09:00:00-04:00", 72, "Rain", true)
        )
        val d = ForecastSummarizer.summarize(p)
        assertEquals(2, d.size)
        assertEquals(70, d[0].high)
        assertEquals(55, d[0].low)
    }

    @Test fun limitsToFiveDays() {
        val periods = (0..6).map { day ->
            Period("2026-05-${20 + day}T09:00:00-04:00", 60 + day, "Cloudy", true)
        }
        val summary = ForecastSummarizer.summarize(periods)
        assertEquals(5, summary.size)
        assertTrue(summary.all { it.summary.isNotBlank() })
    }
}
