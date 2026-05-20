package info.mfields.weather

import info.mfields.weather.network.HourlyResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherGovParsingTest {
 @Test fun parsesHourlyResponse(){
  val sample = """{"properties":{"periods":[{"startTime":"2026-05-20T09:00:00-04:00","temperature":70,"shortForecast":"Sunny","isDaytime":true}]}}"""
  val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<HourlyResponse>(sample)
  assertEquals(1, parsed.properties.periods.size)
 }
}
