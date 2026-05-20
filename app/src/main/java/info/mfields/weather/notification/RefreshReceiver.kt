package info.mfields.weather.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import info.mfields.weather.worker.RefreshWorker

class RefreshReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) { RefreshWorker.oneTime(context) }
}
