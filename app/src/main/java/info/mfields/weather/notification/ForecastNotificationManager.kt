package info.mfields.weather.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import info.mfields.weather.ForecastCache
import info.mfields.weather.MainActivity
import info.mfields.weather.R

class ForecastNotificationManager(private val context: Context) {
    private val nm = context.getSystemService(NotificationManager::class.java)
    fun show(cache: ForecastCache) {
        val channel = NotificationChannel("forecast", "Forecast", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(channel)
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val refreshIntent = PendingIntent.getBroadcast(context, 2, Intent(context, RefreshActionReceiver::class.java), PendingIntent.FLAG_IMMUTABLE)
        val text = cache.daily.joinToString(" • ") { "${it.dayLabel} ${it.high}/${it.low} ${it.summary}" }
        nm.notify(42, NotificationCompat.Builder(context, "forecast").setSmallIcon(android.R.drawable.ic_menu_compass).setContentTitle("Local forecast").setContentText(text.take(120)).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setOngoing(true).setContentIntent(open).addAction(0, "Refresh", refreshIntent).build())
    }
}
