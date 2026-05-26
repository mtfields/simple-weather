package info.mfields.weather.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import info.mfields.weather.data.DaySummary
import info.mfields.weather.data.WeatherRepository
import info.mfields.weather.notification.NotificationHelper
import info.mfields.weather.worker.RefreshWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

data class UiState(
    val location: String? = null,
    val days: List<DaySummary> = emptyList(),
    val lastSuccess: String = "Never",
    val lastFail: String? = null,
    val lastError: String? = null,
    val notificationsEnabled: Boolean = true,
    val backgroundEnabled: Boolean = true,
    val refreshIntervalHours: Int = 3,
    val diagnostics: String = ""
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WeatherRepository(app)
    val state = MutableStateFlow(UiState())

    init {
        viewModelScope.launch {
            repo.observe().collectLatest { s ->
                val now = Instant.now().epochSecond
                val endpointAge = s.endpoint?.let { now - it.fetchedAt }
                val locPermission = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val notifPermission = NotificationManagerCompat.from(getApplication()).areNotificationsEnabled()
                val workInfo = WorkManager.getInstance(getApplication()).getWorkInfosForUniqueWork(RefreshWorker.UNIQUE).get()
                val workState = workInfo.firstOrNull()?.state ?: WorkInfo.State.CANCELLED
                state.value = UiState(
                    s.location?.let { "%.3f, %.3f".format(it.lat, it.lon) },
                    s.forecast?.days ?: emptyList(),
                    s.lastSuccess,
                    s.lastFail,
                    s.lastError,
                    s.notificationsEnabled,
                    s.backgroundEnabled,
                    s.refreshIntervalHours,
                    "endpointUrl=${s.endpoint?.hourlyUrl ?: "missing"}; endpointAgeSec=${endpointAge ?: -1}; grid=${s.endpoint?.gridId ?: "-"}/${s.endpoint?.gridX ?: "-"},${s.endpoint?.gridY ?: "-"}; notifPerm=$notifPermission; locPerm=$locPermission; bgEnabled=${s.backgroundEnabled}; worker=${RefreshWorker.UNIQUE}:$workState; forecastUpdated=${s.forecast?.updatedAt ?: "never"}"
                )
            }
        }
        viewModelScope.launch { ensureBackgroundScheduling() }
    }

    private suspend fun ensureBackgroundScheduling() {
        val appState = repo.observe().first()
        if (appState.backgroundEnabled) RefreshWorker.schedule(getApplication(), appState.refreshIntervalHours.toLong())
        else RefreshWorker.cancelPeriodic(getApplication())
    }

    fun refreshNow() = RefreshWorker.oneTime(getApplication())

    fun setNotificationsEnabled(v: Boolean) {
        viewModelScope.launch {
            repo.setNotifications(v)
            val helper = NotificationHelper(getApplication())
            if (v) helper.updateFromCache() else helper.cancel()
        }
    }

    fun setBackgroundEnabled(v: Boolean) {
        viewModelScope.launch {
            repo.setBackground(v)
            if (v) RefreshWorker.schedule(getApplication(), state.value.refreshIntervalHours.toLong()) else RefreshWorker.cancelPeriodic(getApplication())
        }
    }

    fun setRefreshInterval(hours: Int) {
        viewModelScope.launch {
            repo.setRefreshInterval(hours)
            if (state.value.backgroundEnabled) RefreshWorker.schedule(getApplication(), hours.toLong())
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) return
        val c = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        c.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) viewModelScope.launch {
                repo.setLocation(loc.latitude, loc.longitude)
                refreshNow()
            }
        }
    }
}
