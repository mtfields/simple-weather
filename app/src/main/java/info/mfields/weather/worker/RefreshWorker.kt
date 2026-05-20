package info.mfields.weather.worker

import android.content.Context
import androidx.work.*
import info.mfields.weather.data.WeatherRepository
import info.mfields.weather.notification.NotificationHelper
import java.util.concurrent.TimeUnit

class RefreshWorker(ctx: Context, params: WorkerParameters): CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val repo = WeatherRepository(applicationContext)
        val r = repo.refresh()
        NotificationHelper(applicationContext).updateFromCache()
        return if (r.isSuccess) Result.success() else Result.retry()
    }
    companion object {
        const val UNIQUE = "weather_periodic_refresh"
        fun schedule(context: Context, everyHours: Long = 3) {
            val req = PeriodicWorkRequestBuilder<RefreshWorker>(everyHours, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.UPDATE, req)
        }
        fun oneTime(context: Context) = WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build())
    }
}
