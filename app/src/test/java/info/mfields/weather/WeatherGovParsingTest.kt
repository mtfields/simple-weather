package info.mfields.weather

import info.mfields.weather.network.HourlyResponse
import info.mfields.weather.network.PointsResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherGovParsingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun parsesPointsAndHourlyResponseSamples() {
        val pointsSample = """
            {"properties":{"forecastHourly":"https://api.weather.gov/gridpoints/TOP/31,80/forecast/hourly","gridId":"TOP","gridX":31,"gridY":80}}
        """.trimIndent()
        val points = json.decodeFromString<PointsResponse>(pointsSample)
        assertEquals("TOP", points.properties.gridId)
        assertEquals(31, points.properties.gridX)
        assertEquals(80, points.properties.gridY)
        assertTrue(points.properties.forecastHourly.contains("forecast/hourly"))

        val hourlySample = """
            {"properties":{"periods":[
              {"startTime":"2026-05-20T09:00:00-04:00","temperature":70,"shortForecast":"Sunny","isDaytime":true,"windSpeed":"5 mph"},
              {"startTime":"2026-05-20T10:00:00-04:00","temperature":71,"shortForecast":"Sunny","isDaytime":true,"probabilityOfPrecipitation":{"value":3}}
            ]}}
        """.trimIndent()
        val parsed = json.decodeFromString<HourlyResponse>(hourlySample)
        assertEquals(2, parsed.properties.periods.size)
        assertEquals("Sunny", parsed.properties.periods.first().shortForecast)
    }
}
