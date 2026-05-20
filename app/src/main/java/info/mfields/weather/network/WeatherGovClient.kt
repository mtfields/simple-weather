package info.mfields.weather.network

import info.mfields.weather.data.DaySummary
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.OffsetDateTime

class WeatherGovClient {
    private val c = OkHttpClient()
    private val j = Json { ignoreUnknownKeys = true }
    fun points(lat: Double, lon: Double): String {
        val req = Request.Builder().url("https://api.weather.gov/points/$lat,$lon").header("User-Agent","simple-weather/1.0 (mfields info@mfields.info)").build()
        c.newCall(req).execute().use { r -> return j.decodeFromString<PointsResponse>(r.body!!.string()).properties.forecastHourly }
    }
    fun hourly(url: String): List<Period> {
        val req=Request.Builder().url(url).header("User-Agent","simple-weather/1.0 (mfields info@mfields.info)").build()
        c.newCall(req).execute().use { r -> return j.decodeFromString<HourlyResponse>(r.body!!.string()).properties.periods }
    }
}
@Serializable data class PointsResponse(val properties: PointProperties)
@Serializable data class PointProperties(val forecastHourly: String)
@Serializable data class HourlyResponse(val properties: HourlyProperties)
@Serializable data class HourlyProperties(val periods: List<Period>)
@Serializable data class Period(val startTime: String, val temperature:Int, val shortForecast:String, val isDaytime:Boolean)

object ForecastSummarizer {
    fun summarize(periods: List<Period>): List<DaySummary> = periods.groupBy { OffsetDateTime.parse(it.startTime).toLocalDate() }
        .entries.sortedBy { it.key }.take(5).map { (d, ps) ->
            DaySummary(d.dayOfWeek.name.take(3), ps.maxOf { it.temperature }, ps.minOf { it.temperature }, ps.groupingBy { it.shortForecast }.eachCount().maxByOrNull { it.value }?.key ?: "")
        }
}
