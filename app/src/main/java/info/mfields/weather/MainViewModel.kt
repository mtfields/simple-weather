package info.mfields.weather

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import info.mfields.weather.location.LocationPicker
import info.mfields.weather.notification.ForecastNotificationManager
import info.mfields.weather.work.WorkScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val locator = ServiceLocator(app)
    private val scheduler = WorkScheduler(app)
    private val location = LocationPicker(app)
    val state = locator.store.state.stateIn(viewModelScope, SharingStarted.Eagerly, info.mfields.weather.data.StoredState(null, null, null, true, true, 3, null, null, null))

    init { viewModelScope.launch { val s = state.value; if (s.bgEnabled) scheduler.schedulePeriodic(s.intervalHours) } }
    fun refreshNow() = scheduler.enqueueManualRefresh()
    fun toggleNotif(v:Boolean){viewModelScope.launch{locator.store.setNotif(v); state.value.forecast?.let { ForecastNotificationManager(getApplication()).show(it) } }}
    fun toggleBg(v:Boolean){viewModelScope.launch{ locator.store.setBg(v); if(v) scheduler.schedulePeriodic(state.value.intervalHours) else scheduler.cancelPeriodic() }}
    fun setInterval(h:Int){viewModelScope.launch{ locator.store.setInterval(h); if(state.value.bgEnabled) scheduler.schedulePeriodic(h) }}
    fun useCurrentLocation(){viewModelScope.launch{ location.currentCoarse()?.let { locator.store.saveLocation(it) } }}
    fun hasNotificationPermission(): Boolean = android.os.Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
    fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
}
