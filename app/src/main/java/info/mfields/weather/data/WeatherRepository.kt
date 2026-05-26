package info.mfields.weather.data

import android.content.Context
import info.mfields.weather.network.ForecastSummarizer
import info.mfields.weather.network.WeatherGovClient
import kotlinx.coroutines.flow.first
import java.time.Instant

class WeatherRepository(private val context: Context, private val store: Store = Store(context), private val client: WeatherGovClient = WeatherGovClient()) {
    suspend fun refresh(): Result<Unit> {
        val s = store.state.first()
        val loc = s.location ?: return Result.failure(IllegalStateException("No saved location"))
        return try {
            val cached = s.endpoint
            val isStale = cached == null || cached.hourlyUrl.isBlank() || cached.pointsUrl.isBlank() || (Instant.now().epochSecond - cached.fetchedAt) > 86400
            val endpoint = if (isStale) {
                val points = client.points(loc.lat, loc.lon)
                EndpointCache(
                    pointsUrl = "https://api.weather.gov/points/${loc.lat},${loc.lon}",
                    hourlyUrl = points.forecastHourly,
                    gridId = points.gridId,
                    gridX = points.gridX,
                    gridY = points.gridY,
                    fetchedAt = Instant.now().epochSecond
                )
            } else cached

            val periods = client.hourly(endpoint.hourlyUrl)
            val days = ForecastSummarizer.summarize(periods)
            store.update {
                it.copy(
                    forecast = ForecastCache(days, Instant.now().toString()),
                    endpoint = endpoint,
                    lastSuccess = Instant.now().toString(),
                    lastError = null
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            store.update { it.copy(lastFail = Instant.now().toString(), lastError = e.message) }
            Result.failure(e)
        }
    }

    suspend fun setLocation(lat: Double, lon: Double) { store.update { it.copy(location = SavedLocation(lat, lon)) } }
    fun observe() = store.state
    suspend fun setNotifications(v: Boolean) = store.update { it.copy(notificationsEnabled = v) }
    suspend fun setBackground(v: Boolean) = store.update { it.copy(backgroundEnabled = v) }
    suspend fun setRefreshInterval(hours: Int) = store.update { it.copy(refreshIntervalHours = hours) }
}
