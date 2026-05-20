package info.mfields.weather.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import info.mfields.weather.work.WorkScheduler

class RefreshActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        WorkScheduler(context).enqueueManualRefresh()
    }
}
