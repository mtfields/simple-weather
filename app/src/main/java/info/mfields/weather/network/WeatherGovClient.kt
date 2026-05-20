package info.mfields.weather.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class WeatherGovClient(private val client: OkHttpClient = OkHttpClient()) {
    private val json = Json { ignoreUnknownKeys = true }
    private val userAgent = "SimpleWeather/1.0 (info.mfields.weather; mfields personal app)"

    fun points(lat: Double, lon: Double): PointsResponse {
        return get("https://api.weather.gov/points/$lat,$lon", PointsResponse.serializer())
    }

    fun hourly(url: String): HourlyForecastResponse = get(url, HourlyForecastResponse.serializer())

    private fun <T> get(url: String, deserializer: kotlinx.serialization.DeserializationStrategy<T>): T {
        val req = Request.Builder().url(url).header("User-Agent", userAgent).build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("HTTP ${res.code} for $url")
            return json.decodeFromString(deserializer, res.body!!.string())
        }
    }
}

@Serializable data class PointsResponse(val properties: PointsProps)
@Serializable data class PointsProps(
    @SerialName("forecastHourly") val forecastHourly: String,
    @SerialName("gridId") val gridId: String,
    @SerialName("gridX") val gridX: Int,
    @SerialName("gridY") val gridY: Int,
)
@Serializable data class HourlyForecastResponse(val properties: HourlyProps)
@Serializable data class HourlyProps(val periods: List<HourlyPeriod>)
@Serializable data class HourlyPeriod(
    val startTime: String,
    val temperature: Int,
    val shortForecast: String,
    val isDaytime: Boolean,
)
