package info.mfields.weather.data

import kotlinx.serialization.Serializable

@Serializable data class SavedLocation(val lat: Double, val lon: Double)
@Serializable data class DaySummary(val day: String, val high: Int, val low: Int, val summary: String)
@Serializable data class ForecastCache(val days: List<DaySummary>, val updatedAt: String)
@Serializable data class EndpointCache(
    val pointsUrl: String = "",
    val hourlyUrl: String = "",
    val gridId: String? = null,
    val gridX: Int? = null,
    val gridY: Int? = null,
    val fetchedAt: Long = 0
)
@Serializable data class AppState(
    val location: SavedLocation? = null,
    val forecast: ForecastCache? = null,
    val endpoint: EndpointCache? = null,
    val refreshIntervalHours: Int = 3,
    val lastSuccess: String = "Never",
    val lastFail: String? = null,
    val lastError: String? = null,
    val notificationsEnabled: Boolean = true,
    val backgroundEnabled: Boolean = true
)
