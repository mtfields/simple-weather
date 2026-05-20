package info.mfields.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherParsingTest {
    @Test fun parseHourlyLikeWeatherGov() {
        val repo = WeatherRepoForTest()
        val periods = repo.parse("""
            {"properties":{"periods":[
              {"startTime":"2026-05-20T01:00:00-04:00","temperature":55,"shortForecast":"Clear","isDaytime":false},
              {"startTime":"2026-05-20T02:00:00-04:00","temperature":54,"shortForecast":"Clear","isDaytime":false}
            ]}}
        """.trimIndent())
        assertEquals(2, periods.size)
        assertEquals(55, periods.first().temperature)
    }
}

private class WeatherRepoForTest {
    fun parse(body: String): List<HourlyPeriod> {
        val arr = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject["properties"]!!.jsonObject["periods"]!!.jsonArray
        return arr.map { p -> val o = p.jsonObject; HourlyPeriod(o["startTime"]!!.toString().trim('"'), o["temperature"]!!.toString().toInt(), o["shortForecast"]!!.toString().trim('"'), o["isDaytime"]!!.toString().toBoolean()) }
    }
}
