package info.mfields.weather.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import info.mfields.weather.MainActivity
import info.mfields.weather.data.Store
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class NotificationHelper(private val c: Context) {
    private val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    fun updateFromCache() = runBlocking {
        val s = Store(c).state.first()
        if (!s.notificationsEnabled) {
            cancel()
            return@runBlocking
        }
        val ch = NotificationChannel("forecast", "Forecast", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(ch)
        val text = s.forecast?.days?.take(3)?.joinToString(" • ") { "${it.day} ${it.high}/${it.low} ${it.summary}" } ?: "No forecast cached"
        val open = PendingIntent.getActivity(c, 1, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val refresh = PendingIntent.getBroadcast(c, 2, Intent(c, RefreshReceiver::class.java), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(7, NotificationCompat.Builder(c, "forecast").setContentTitle("Local forecast").setContentText(text).setSmallIcon(android.R.drawable.ic_menu_compass).setOngoing(true).setContentIntent(open).addAction(0, "Refresh", refresh).build())
    }

    fun cancel() = nm.cancel(7)
}
