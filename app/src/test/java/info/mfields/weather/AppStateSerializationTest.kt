package info.mfields.weather

import info.mfields.weather.data.AppState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AppStateSerializationTest {
    @Test fun refreshIntervalPersistsInStateJson() {
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val encoded = json.encodeToString(AppState(refreshIntervalHours = 2))
        val decoded = json.decodeFromString<AppState>(encoded)
        assertEquals(2, decoded.refreshIntervalHours)
    }
}
