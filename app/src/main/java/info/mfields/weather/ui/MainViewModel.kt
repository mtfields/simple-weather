package info.mfields.weather.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
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

data class UiState(val location:String?=null, val days:List<DaySummary> = emptyList(), val lastSuccess:String="Never", val lastFail:String?=null, val lastError:String?=null, val notificationsEnabled:Boolean=true, val backgroundEnabled:Boolean=true, val refreshIntervalHours: Long = 3, val diagnostics:String="")

class MainViewModel(app: Application): AndroidViewModel(app) {
    private val repo = WeatherRepository(app)
    val state = MutableStateFlow(UiState())

    init {
        viewModelScope.launch {
            repo.observe().collectLatest { s ->
                val wmState = WorkManager.getInstance(getApplication()).getWorkInfosForUniqueWork(RefreshWorker.UNIQUE).get().firstOrNull()?.state ?: WorkInfo.State.CANCELLED
                val locationPermission = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                val notificationPermission = android.os.Build.VERSION.SDK_INT < 33 || hasPermission(Manifest.permission.POST_NOTIFICATIONS)
                val endpoint = s.endpoint
                val endpointStatus = if (endpoint == null) "missing" else "hourly=${endpoint.hourlyUrl}, stale=${(System.currentTimeMillis()/1000 - endpoint.fetchedAt) > 86400}"
                state.value = UiState(
                    s.location?.let{"%.3f, %.3f".format(it.lat,it.lon)}, s.forecast?.days ?: emptyList(), s.lastSuccess, s.lastFail, s.lastError, s.notificationsEnabled, s.backgroundEnabled, s.refreshIntervalHours,
                    "locationPerm=$locationPermission notifPerm=$notificationPermission endpoint=$endpointStatus bg=${s.backgroundEnabled} worker=${wmState.name}"
                )
            }
        }
        viewModelScope.launch {
            val s = repo.observe().first()
            if (s.backgroundEnabled) RefreshWorker.schedule(getApplication(), s.refreshIntervalHours) else RefreshWorker.cancel(getApplication())
            NotificationHelper(getApplication()).updateFromCache()
        }
    }
    fun refreshNow() = RefreshWorker.oneTime(getApplication())
    fun setNotificationsEnabled(v:Boolean){ viewModelScope.launch { repo.setNotifications(v); if (!v) NotificationHelper(getApplication()).cancel() else NotificationHelper(getApplication()).updateFromCache() } }
    fun setBackgroundEnabled(v:Boolean){ viewModelScope.launch { repo.setBackground(v); if (v) RefreshWorker.scheduleFromSettings(getApplication()) else RefreshWorker.cancel(getApplication()) } }
    fun setRefreshIntervalHours(v: Long){ viewModelScope.launch { repo.setRefreshIntervalHours(v); if (state.value.backgroundEnabled) RefreshWorker.schedule(getApplication(), v) } }
    fun onLocationPermissionResult(granted:Boolean){ if (!granted) return; val c = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        c.lastLocation.addOnSuccessListener { loc -> if (loc!=null) viewModelScope.launch { repo.setLocation(loc.latitude, loc.longitude); refreshNow() } }
    }
    private fun hasPermission(permission: String): Boolean = ContextCompat.checkSelfPermission(getApplication(), permission) == PackageManager.PERMISSION_GRANTED
}
