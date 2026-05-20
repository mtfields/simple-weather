package info.mfields.weather.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import info.mfields.weather.data.DaySummary
import info.mfields.weather.data.WeatherRepository
import info.mfields.weather.notification.NotificationHelper
import info.mfields.weather.worker.RefreshWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class UiState(val location:String?=null, val days:List<DaySummary> = emptyList(), val lastSuccess:String="Never", val lastFail:String?=null, val lastError:String?=null, val notificationsEnabled:Boolean=true, val backgroundEnabled:Boolean=true, val diagnostics:String="")

class MainViewModel(app: Application): AndroidViewModel(app) {
    private val repo = WeatherRepository(app)
    val state = MutableStateFlow(UiState())
    init { viewModelScope.launch { repo.observe().collectLatest { s -> state.value = UiState(
        s.location?.let{"%.3f, %.3f".format(it.lat,it.lon)}, s.forecast?.days ?: emptyList(), s.lastSuccess, s.lastFail, s.lastError, s.notificationsEnabled, s.backgroundEnabled,
        "endpoint=${s.endpoint?.hourlyUrl ?: "missing"}, updated=${s.forecast?.updatedAt ?: "never"}")
        if (s.backgroundEnabled) RefreshWorker.schedule(getApplication())
        NotificationHelper(getApplication()).updateFromCache()
    } } }
    fun refreshNow() = RefreshWorker.oneTime(getApplication())
    fun setNotificationsEnabled(v:Boolean){ viewModelScope.launch { repo.setNotifications(v); NotificationHelper(getApplication()).updateFromCache() } }
    fun setBackgroundEnabled(v:Boolean){ viewModelScope.launch { repo.setBackground(v); if (v) RefreshWorker.schedule(getApplication()) } }
    fun onLocationPermissionResult(granted:Boolean){ if (!granted) return; val c = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        c.lastLocation.addOnSuccessListener { loc -> if (loc!=null) viewModelScope.launch { repo.setLocation(loc.latitude, loc.longitude); refreshNow() } }
    }
}
