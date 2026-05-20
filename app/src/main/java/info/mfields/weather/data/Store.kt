package info.mfields.weather.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.ds by preferencesDataStore("weather")
class Store(private val c: Context) {
    private val key = stringPreferencesKey("state")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val state: Flow<AppState> = c.ds.data.map { p -> p[key]?.let { json.decodeFromString<AppState>(it) } ?: AppState() }
    suspend fun update(t: (AppState)->AppState){
        val cur = state.first()
        c.ds.edit { it[key] = json.encodeToString(t(cur)) }
    }
}
