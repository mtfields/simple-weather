package info.mfields.weather

import android.app.Application
import androidx.work.WorkManager
import info.mfields.weather.worker.RefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(this, androidx.work.Configuration.Builder().build())
        CoroutineScope(Dispatchers.Default).launch { RefreshWorker.scheduleFromState(this@App) }
    }
}
