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
            val now = Instant.now().epochSecond
            val endpoint = s.endpoint
            val freshEndpoint = if (endpoint == null || endpoint.hourlyUrl.isBlank() || (now - endpoint.fetchedAt) > 86400) {
                client.points(loc.lat, loc.lon).copy(fetchedAt = now)
            } else endpoint
            val periods = client.hourly(freshEndpoint.hourlyUrl)
            val days = ForecastSummarizer.summarize(periods)
            store.update { it.copy(forecast = ForecastCache(days, Instant.now().toString()), endpoint = freshEndpoint, lastSuccess = Instant.now().toString(), lastError = null) }
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
    suspend fun setRefreshIntervalHours(v: Int) = store.update { it.copy(refreshIntervalHours = if (v == 2) 2 else 3) }
}
