package info.mfields.weather.work

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class WorkScheduler(private val context: Context) {
    fun schedulePeriodic(hours: Int) {
        val req = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(hours.toLong(), TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("forecast_periodic", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
    fun cancelPeriodic() = WorkManager.getInstance(context).cancelUniqueWork("forecast_periodic")
    fun enqueueManualRefresh() {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<WeatherRefreshWorker>().build())
    }
}
