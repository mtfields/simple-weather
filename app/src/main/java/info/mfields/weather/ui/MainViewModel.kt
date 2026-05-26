package info.mfields.weather.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
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
            ensurePeriodicScheduleFromState()
            repo.observe().collectLatest { s ->
                state.value = UiState(
                    location = s.location?.let { "%.3f, %.3f".format(it.lat, it.lon) },
                    days = s.forecast?.days ?: emptyList(),
                    lastSuccess = s.lastSuccess,
                    lastFail = s.lastFail,
                    lastError = s.lastError,
                    notificationsEnabled = s.notificationsEnabled,
                    backgroundEnabled = s.backgroundEnabled,
                    refreshIntervalHours = s.refreshIntervalHours,
                    diagnostics = diagnosticsString(s)
                )
                NotificationHelper(getApplication()).updateFromCache()
            }
        }
    }

    fun refreshNow() = RefreshWorker.oneTime(getApplication())

    fun setNotificationsEnabled(v: Boolean) {
        viewModelScope.launch {
            repo.setNotifications(v)
            val helper = NotificationHelper(getApplication())
            if (v) helper.updateFromCache() else helper.cancelForecastNotification()
        }
    }

    fun setBackgroundEnabled(v: Boolean) {
        viewModelScope.launch {
            repo.setBackground(v)
            val s = repo.currentState()
            if (v) RefreshWorker.schedule(getApplication(), s.refreshIntervalHours.toLong()) else RefreshWorker.cancelPeriodic(getApplication())
        }
    }

    fun setRefreshIntervalHours(v: Int) {
        viewModelScope.launch {
            repo.setRefreshIntervalHours(v)
            val s = repo.currentState()
            if (s.backgroundEnabled) RefreshWorker.schedule(getApplication(), v.toLong())
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

    private suspend fun ensurePeriodicScheduleFromState() {
        val s = repo.currentState()
        if (s.backgroundEnabled) RefreshWorker.schedule(getApplication(), s.refreshIntervalHours.toLong()) else RefreshWorker.cancelPeriodic(getApplication())
    }

    private fun diagnosticsString(snapshot: info.mfields.weather.data.AppState): String {
        val app = getApplication<Application>()
        val notifGranted = if (Build.VERSION.SDK_INT >= 33) ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true
        val locationGranted = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val endpoint = snapshot.endpoint
        val endpointAge = endpoint?.fetchedAt?.let { Instant.now().epochSecond - it }
        val endpointStatus = if (endpoint == null) "missing" else "cached(age=${endpointAge}s, stale=${endpointAge != null && endpointAge > 86400})"
        val workerState = runCatching {
            val infos = WorkManager.getInstance(app).getWorkInfosForUniqueWork(RefreshWorker.UNIQUE).get()
            infos.joinToString { it.state.name }
        }.getOrDefault("unknown")
        return "savedLocation=${snapshot.location?.lat},${snapshot.location?.lon}; lastSuccess=${snapshot.lastSuccess}; lastFail=${snapshot.lastFail}; lastError=${snapshot.lastError}; notifPerm=$notifGranted; locPerm=$locationGranted; endpoint=$endpointStatus; bgEnabled=${snapshot.backgroundEnabled}; worker=${RefreshWorker.UNIQUE}:$workerState"
    }
}
