package info.mfields.weather.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import info.mfields.weather.data.DaySummary
import info.mfields.weather.data.WeatherRepository
import info.mfields.weather.notification.NotificationHelper
import info.mfields.weather.worker.RefreshWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
            RefreshWorker.scheduleFromState(getApplication())
            repo.observe().collectLatest { s ->
                val notifPerm = android.os.Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                val locPerm = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val endpointAge = s.endpoint?.let { (System.currentTimeMillis() / 1000L) - it.fetchedAt }
                val workInfo = WorkManager.getInstance(getApplication()).getWorkInfosForUniqueWork(RefreshWorker.UNIQUE).get().firstOrNull()?.state
                state.value = UiState(
                    s.location?.let { "%.3f, %.3f".format(it.lat, it.lon) }, s.forecast?.days ?: emptyList(), s.lastSuccess, s.lastFail, s.lastError,
                    s.notificationsEnabled, s.backgroundEnabled, s.refreshIntervalHours,
                    "locationPerm=$locPerm notifPerm=$notifPerm endpoint=${s.endpoint?.hourlyUrl ?: "missing"} endpointAgeSec=${endpointAge ?: -1} bg=${s.backgroundEnabled} interval=${s.refreshIntervalHours}h worker=$workInfo"
                )
            }
        }
    }

    fun refreshNow() = RefreshWorker.oneTime(getApplication())
    fun setNotificationsEnabled(v: Boolean) { viewModelScope.launch { repo.setNotifications(v); NotificationHelper(getApplication()).updateFromCache() } }
    fun setBackgroundEnabled(v: Boolean) { viewModelScope.launch { repo.setBackground(v); if (v) RefreshWorker.scheduleFromState(getApplication()) else RefreshWorker.cancel(getApplication()) } }
    fun setRefreshInterval(v: Int) { viewModelScope.launch { repo.setRefreshIntervalHours(v); RefreshWorker.scheduleFromState(getApplication()) } }

    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) return
        val c = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        c.lastLocation.addOnSuccessListener { loc -> if (loc != null) viewModelScope.launch { repo.setLocation(loc.latitude, loc.longitude); refreshNow() } }
    }
}
