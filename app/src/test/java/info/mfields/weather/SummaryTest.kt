package info.mfields.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryTest {
    @Test fun summarize_groupsByDay() {
        val repo = WeatherRepository(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val out = repo.summarize(listOf(
            HourPeriod("2026-05-20T01:00:00-04:00",70,"F","Clear"),
            HourPeriod("2026-05-20T03:00:00-04:00",60,"F","Cloudy"),
            HourPeriod("2026-05-20T06:00:00-04:00",75,"F","Clear")
        ))
        assertEquals(1, out.size)
        assertEquals(75, out[0].high)
        assertEquals(60, out[0].low)
    }

    @Test fun parsePeriods_extractsRows() {
        val repo = WeatherRepository(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val json = """{"properties":{"periods":[{"startTime":"2026-05-20T01:00:00-04:00","temperature":70,"temperatureUnit":"F","shortForecast":"Clear"}]}}"""
        assertEquals(1, repo.parsePeriods(json).size)
    }
}
