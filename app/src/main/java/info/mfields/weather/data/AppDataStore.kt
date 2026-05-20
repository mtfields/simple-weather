package info.mfields.weather.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import info.mfields.weather.DailySummary
import info.mfields.weather.EndpointMetadata
import info.mfields.weather.ForecastCache
import info.mfields.weather.SavedLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

private val Context.ds by preferencesDataStore("weather")

class AppDataStore(private val context: Context) {
    private object K {
        val lat = doublePreferencesKey("lat"); val lon = doublePreferencesKey("lon")
        val hourlyUrl = stringPreferencesKey("hourly_url"); val pointsUrl = stringPreferencesKey("points_url")
        val metaUpdated = longPreferencesKey("meta_updated")
        val forecastPayload = stringPreferencesKey("forecast_payload")
        val forecastUpdated = longPreferencesKey("forecast_updated")
        val notif = booleanPreferencesKey("notif_enabled"); val bg = booleanPreferencesKey("bg_enabled")
        val intervalH = intPreferencesKey("interval_h")
        val lastSuccess = longPreferencesKey("last_success"); val lastFail = longPreferencesKey("last_fail"); val lastError = stringPreferencesKey("last_error")
    }
    val state: Flow<StoredState> = context.ds.data.map { p ->
        StoredState(
            location = if (p[K.lat] != null && p[K.lon] != null) SavedLocation(p[K.lat]!!, p[K.lon]!!) else null,
            metadata = if (p[K.hourlyUrl] != null && p[K.pointsUrl] != null && p[K.metaUpdated] != null) EndpointMetadata(p[K.pointsUrl]!!, p[K.hourlyUrl]!!, Instant.ofEpochMilli(p[K.metaUpdated]!!)) else null,
            forecast = p[K.forecastPayload]?.let { JsonCodec.decodeForecast(it, p[K.forecastUpdated] ?: 0L) },
            notifEnabled = p[K.notif] ?: true,
            bgEnabled = p[K.bg] ?: true,
            intervalHours = p[K.intervalH] ?: 3,
            lastSuccess = p[K.lastSuccess]?.let { Instant.ofEpochMilli(it) },
            lastFail = p[K.lastFail]?.let { Instant.ofEpochMilli(it) },
            lastError = p[K.lastError]
        )
    }
    suspend fun saveLocation(l: SavedLocation) { context.ds.edit { it[K.lat] = l.latitude; it[K.lon] = l.longitude } }
    suspend fun saveMeta(m: EndpointMetadata) { context.ds.edit { it[K.hourlyUrl]=m.hourlyUrl; it[K.pointsUrl]=m.pointsUrl; it[K.metaUpdated]=m.updatedAt.toEpochMilli() } }
    suspend fun saveForecast(f: ForecastCache) { context.ds.edit { it[K.forecastPayload]=JsonCodec.encodeForecast(f.daily); it[K.forecastUpdated]=f.generatedAt.toEpochMilli(); it[K.lastSuccess]=f.generatedAt.toEpochMilli(); it.remove(K.lastError) } }
    suspend fun saveFailure(msg: String, at: Instant) { context.ds.edit { it[K.lastFail]=at.toEpochMilli(); it[K.lastError]=msg } }
    suspend fun setNotif(v:Boolean){context.ds.edit{it[K.notif]=v}}
    suspend fun setBg(v:Boolean){context.ds.edit{it[K.bg]=v}}
    suspend fun setInterval(h:Int){context.ds.edit{it[K.intervalH]=h.coerceIn(2,6)}}
}

data class StoredState(val location: SavedLocation?, val metadata: EndpointMetadata?, val forecast: ForecastCache?, val notifEnabled:Boolean, val bgEnabled:Boolean, val intervalHours:Int, val lastSuccess: Instant?, val lastFail: Instant?, val lastError:String?)
object JsonCodec {
    fun encodeForecast(items: List<DailySummary>) = items.joinToString("|") { "${it.dayLabel},${it.high},${it.low},${it.summary.replace(',', ';')}" }
    fun decodeForecast(payload: String, updated: Long) = ForecastCache(Instant.ofEpochMilli(updated), payload.split("|").filter { it.isNotBlank() }.map {
        val p = it.split(",", limit = 4); DailySummary(p[0], p[1].toInt(), p[2].toInt(), p[3])
    })
}
