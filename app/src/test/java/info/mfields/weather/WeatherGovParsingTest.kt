package info.mfields.weather

import info.mfields.weather.network.WeatherGovClient
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherGovParsingTest {
    @Test fun parsesHourlyResponse() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"properties":{"periods":[{"startTime":"2026-05-20T01:00:00-04:00","temperature":60,"shortForecast":"Cloudy","isDaytime":false}]}}"""))
        server.start()
        val client = WeatherGovClient(OkHttpClient())
        val hourly = client.hourly(server.url("/hourly").toString())
        assertEquals(1, hourly.properties.periods.size)
        server.shutdown()
    }
}
