package info.mfields.weather.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import info.mfields.weather.ServiceLocator
import info.mfields.weather.notification.ForecastNotificationManager

class WeatherRefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val locator = ServiceLocator(applicationContext)
        val res = locator.repo.refresh()
        return if (res.isSuccess) {
            ForecastNotificationManager(applicationContext).show(res.getOrThrow())
            Result.success()
        } else Result.retry()
    }
}
