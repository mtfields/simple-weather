package info.mfields.weather

import info.mfields.weather.network.ForecastSummarizer
import info.mfields.weather.network.Period
import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastSummarizerTest {
    @Test
    fun summarizesByDayWithHighLowAndDominantCondition() {
        val p = listOf(
            Period("2026-05-20T09:00:00-04:00", 70, "Sunny", true),
            Period("2026-05-20T12:00:00-04:00", 73, "Sunny", true),
            Period("2026-05-20T21:00:00-04:00", 55, "Clear", false),
            Period("2026-05-21T09:00:00-04:00", 72, "Rain", true)
        )
        val d = ForecastSummarizer.summarize(p)
        assertEquals(2, d.size)
        assertEquals(73, d[0].high)
        assertEquals(55, d[0].low)
        assertEquals("Sunny", d[0].summary)
    }
}
