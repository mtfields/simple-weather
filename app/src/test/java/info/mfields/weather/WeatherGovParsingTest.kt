package info.mfields.weather

import info.mfields.weather.network.HourlyResponse
import info.mfields.weather.network.PointsResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherGovParsingTest {
 @Test fun parsesPointsAndHourlyResponses(){
  val points = """{"properties":{"gridId":"LWX","gridX":97,"gridY":71,"forecastHourly":"https://api.weather.gov/gridpoints/LWX/97,71/forecast/hourly"}}"""
  val hourly = """{"properties":{"periods":[{"startTime":"2026-05-20T09:00:00-04:00","temperature":70,"shortForecast":"Sunny","isDaytime":true},{"startTime":"2026-05-20T10:00:00-04:00","temperature":72,"shortForecast":"Partly Sunny","isDaytime":true}]}}"""
  val json = Json { ignoreUnknownKeys = true }
  val parsedPoints = json.decodeFromString<PointsResponse>(points)
  val parsedHourly = json.decodeFromString<HourlyResponse>(hourly)
  assertEquals("LWX", parsedPoints.properties.gridId)
  assertTrue(parsedPoints.properties.forecastHourly.contains("forecast/hourly"))
  assertEquals(2, parsedHourly.properties.periods.size)
 }
}
