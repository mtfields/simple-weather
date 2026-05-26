package info.mfields.weather

import info.mfields.weather.network.HourlyResponse
import info.mfields.weather.network.PointsResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherGovParsingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesPointsAndHourlyResponses() {
        val points = """{"properties":{"forecastHourly":"https://api.weather.gov/gridpoints/BOX/70,76/forecast/hourly","gridId":"BOX","gridX":70,"gridY":76}}"""
        val hourly = """{"properties":{"periods":[{"startTime":"2026-05-20T09:00:00-04:00","temperature":70,"shortForecast":"Sunny","isDaytime":true},{"startTime":"2026-05-20T21:00:00-04:00","temperature":55,"shortForecast":"Clear","isDaytime":false}]}}"""

        val parsedPoints = json.decodeFromString<PointsResponse>(points)
        val parsedHourly = json.decodeFromString<HourlyResponse>(hourly)

        assertEquals("BOX", parsedPoints.properties.gridId)
        assertTrue(parsedPoints.properties.forecastHourly.contains("forecast/hourly"))
        assertEquals(2, parsedHourly.properties.periods.size)
        assertEquals(55, parsedHourly.properties.periods.last().temperature)
    }
}
