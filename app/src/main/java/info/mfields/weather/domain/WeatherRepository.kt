package info.mfields.weather.domain

import info.mfields.weather.EndpointMetadata
import info.mfields.weather.ForecastCache
import info.mfields.weather.data.AppDataStore
import info.mfields.weather.network.WeatherGovClient
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant

class WeatherRepository(private val store: AppDataStore, private val client: WeatherGovClient) {
    suspend fun refresh(): Result<ForecastCache> {
        val state = store.state.first()
        val loc = state.location ?: return Result.failure(IllegalStateException("No saved location"))
        return try {
            val meta = state.metadata?.takeIf { Duration.between(it.updatedAt, Instant.now()).toHours() < 24 } ?: run {
                val p = client.points(loc.latitude, loc.longitude)
                EndpointMetadata("/points/${loc.latitude},${loc.longitude}", p.properties.forecastHourly, Instant.now()).also { store.saveMeta(it) }
            }
            val hourly = client.hourly(meta.hourlyUrl)
            val cache = ForecastCache(Instant.now(), Summarizer.daily(hourly.properties.periods))
            store.saveForecast(cache)
            Result.success(cache)
        } catch (e: Exception) {
            store.saveFailure(e.message ?: "Unknown", Instant.now())
            Result.failure(e)
        }
    }
}
