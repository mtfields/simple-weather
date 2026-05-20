package info.mfields.weather

import android.app.Application
import androidx.work.WorkManager

class App: Application() {
    override fun onCreate() { super.onCreate(); WorkManager.initialize(this, androidx.work.Configuration.Builder().build()) }
}
