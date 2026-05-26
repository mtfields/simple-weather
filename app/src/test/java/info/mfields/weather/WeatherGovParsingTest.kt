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
        val pointsSample = """
            {"properties":{"forecastHourly":"https://api.weather.gov/gridpoints/SEW/124,67/forecast/hourly","gridId":"SEW","gridX":124,"gridY":67,"extra":"ignored"}}
        """.trimIndent()
        val points = json.decodeFromString<PointsResponse>(pointsSample)
        assertEquals("SEW", points.properties.gridId)
        assertEquals(124, points.properties.gridX)

        val hourlySample = """
            {"properties":{"periods":[
              {"startTime":"2026-05-20T09:00:00-04:00","temperature":70,"shortForecast":"Sunny","isDaytime":true},
              {"startTime":"2026-05-20T10:00:00-04:00","temperature":71,"shortForecast":"Sunny","isDaytime":true}
            ]}}
        """.trimIndent()
        val hourly = json.decodeFromString<HourlyResponse>(hourlySample)
        assertEquals(2, hourly.properties.periods.size)
        assertTrue(hourly.properties.periods.all { it.shortForecast == "Sunny" })
    }
}
