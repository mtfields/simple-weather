package info.mfields.weather

import java.time.Instant

data class SavedLocation(val latitude: Double, val longitude: Double)
data class DailySummary(val dayLabel: String, val high: Int, val low: Int, val summary: String)
data class ForecastCache(val generatedAt: Instant, val daily: List<DailySummary>)
data class EndpointMetadata(val pointsUrl: String, val hourlyUrl: String, val updatedAt: Instant)
