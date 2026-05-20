package info.mfields.weather

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

val Context.dataStore by preferencesDataStore("weather_store")

class WeatherApp : Application() { override fun onCreate() { super.onCreate(); ensureChannel(this) } }
class MainActivity : ComponentActivity() {
    private val vm by viewModels<WeatherViewModel>()
    override fun onCreate(savedInstanceState: android.os.Bundle?) { super.onCreate(savedInstanceState); setContent { WeatherScreen(vm) } }
}

@Serializable data class HourlyPeriod(val startTime: String, val temperature: Int, val shortForecast: String, val isDaytime: Boolean)
@Serializable data class CachedMeta(val forecastHourlyUrl: String, val gridId: String, val gridX: Int, val gridY: Int, val updatedEpoch: Long)
@Serializable data class DailyRow(val dayLabel: String, val high: Int, val low: Int, val summary: String)
@Serializable data class CachedForecast(val rows: List<DailyRow>, val updatedEpoch: Long)

object Keys { val lat = doublePreferencesKey("lat"); val lon = doublePreferencesKey("lon"); val meta = stringPreferencesKey("meta"); val forecast = stringPreferencesKey("forecast"); val lastSuccess = longPreferencesKey("last_success"); val lastFail = longPreferencesKey("last_fail"); val lastErr = stringPreferencesKey("last_err"); val notif = booleanPreferencesKey("notif"); val bg = booleanPreferencesKey("bg"); val interval = intPreferencesKey("interval") }
class WeatherRepo(private val ctx: Context) {
    private val client = OkHttpClient(); private val json = Json { ignoreUnknownKeys = true }
    suspend fun refreshNow(): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) { runCatching {
        val p = ctx.dataStore.data.first(); val lat = p[Keys.lat] ?: error("Location not set"); val lon = p[Keys.lon] ?: error("Location not set")
        val meta = p[Keys.meta]?.let { json.decodeFromString<CachedMeta>(it) }?.takeIf { Instant.now().epochSecond - it.updatedEpoch < 86400 }
        val liveMeta = meta ?: fetchMeta(lat, lon)
        val hours = fetchHourly(liveMeta.forecastHourlyUrl)
        val rows = summarize(hours)
        val cached = CachedForecast(rows, Instant.now().epochSecond)
        ctx.dataStore.edit { e -> e[Keys.meta] = json.encodeToString(CachedMeta.serializer(), liveMeta.copy(updatedEpoch = Instant.now().epochSecond)); e[Keys.forecast] = json.encodeToString(CachedForecast.serializer(), cached); e[Keys.lastSuccess]=Instant.now().epochSecond; e.remove(Keys.lastErr) }
        WeatherNotifier.update(ctx, cached)
    } }.onFailure { ctx.dataStore.edit { it[Keys.lastFail]=Instant.now().epochSecond; it[Keys.lastErr]=it.message ?: "unknown" } }
    private fun fetchMeta(lat: Double, lon: Double): CachedMeta {
        val req = Request.Builder().url("https://api.weather.gov/points/$lat,$lon").header("User-Agent", "simple-weather info.mfields.weather").build()
        val body = client.newCall(req).execute().use { it.body!!.string() }
        val obj = json.parseToJsonElement(body).jsonObject["properties"]!!.jsonObject
        return CachedMeta(obj["forecastHourly"]!!.toString().trim('"'), obj["gridId"]!!.toString().trim('"'), obj["gridX"]!!.toString().toInt(), obj["gridY"]!!.toString().toInt(), Instant.now().epochSecond)
    }
    fun fetchHourly(url: String): List<HourlyPeriod> {
        val req = Request.Builder().url(url).header("User-Agent", "simple-weather info.mfields.weather").build()
        val body = client.newCall(req).execute().use { it.body!!.string() }
        val arr = json.parseToJsonElement(body).jsonObject["properties"]!!.jsonObject["periods"]!!.jsonArray
        return arr.map { p -> val o = p.jsonObject; HourlyPeriod(o["startTime"]!!.toString().trim('"'), o["temperature"]!!.toString().toInt(), o["shortForecast"]!!.toString().trim('"'), o["isDaytime"]!!.toString().toBoolean()) }
    }
}
fun summarize(hours: List<HourlyPeriod>): List<DailyRow> = hours.groupBy { OffsetDateTime.parse(it.startTime).toLocalDate() }.entries.sortedBy { it.key }.take(5).map {
    val v=it.value; val high=v.maxOf { p->p.temperature }; val low=v.minOf { p->p.temperature }; val summary=v.groupingBy { p->p.shortForecast }.eachCount().maxByOrNull { p->p.value }?.key ?: "-"; DailyRow(it.key.dayOfWeek.name.take(3), high, low, summary)
}

class WeatherViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WeatherRepo(app)
    val state = app.dataStore.data.map { p -> UiState(p[Keys.lat], p[Keys.lon], p[Keys.forecast]?.let { Json.decodeFromString<CachedForecast>(it) }, p[Keys.lastSuccess], p[Keys.lastFail], p[Keys.lastErr], p[Keys.notif] ?: true, p[Keys.bg] ?: true, p[Keys.interval] ?: 3, p[Keys.meta]?.isNotBlank()==true) }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())
    init { viewModelScope.launch { schedule(getApplication(), state.value.bgEnabled, state.value.intervalHours) } }
    fun refresh() { viewModelScope.launch { repo.refreshNow() } }
    fun setNotif(v:Boolean){viewModelScope.launch{getApplication<Application>().dataStore.edit{it[Keys.notif]=v}; if(v) refresh() else NotificationManagerCompat.from(getApplication()).cancel(1001)} }
    fun setBg(v:Boolean){viewModelScope.launch{getApplication<Application>().dataStore.edit{it[Keys.bg]=v}; schedule(getApplication(),v,state.value.intervalHours)} }
    fun setInterval(v:Int){viewModelScope.launch{getApplication<Application>().dataStore.edit{it[Keys.interval]=v}; schedule(getApplication(),state.value.bgEnabled,v)} }
    fun setLocation(lat:Double,lon:Double){viewModelScope.launch{getApplication<Application>().dataStore.edit{it[Keys.lat]=lat;it[Keys.lon]=lon}} }
}
data class UiState(val lat:Double?=null,val lon:Double?=null,val forecast:CachedForecast?=null,val lastSuccess:Long?=null,val lastFail:Long?=null,val lastErr:String?=null,val notificationsEnabled:Boolean=true,val bgEnabled:Boolean=true,val intervalHours:Int=3,val hasMeta:Boolean=false)

@Composable fun WeatherScreen(vm: WeatherViewModel) { val s by vm.state.collectAsState(); val c= LocalContext.current
    val locPerm = ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
    val notifPerm = Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
    Scaffold(topBar={TopAppBar(title={Text("Simple Weather")})}){pad->LazyColumn(Modifier.padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item { Text("Saved location: ${s.lat?.let{"%.3f, %.3f".format(it,s.lon)} ?: "Not set"}") }
        item { Row{ Button(onClick={ if(!locPerm) locLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) else LocationServices.getFusedLocationProviderClient(c).lastLocation.addOnSuccessListener{ l-> if(l!=null) vm.setLocation(l.latitude,l.longitude) }}){Text("Use Current Location")}; Spacer(Modifier.width(8.dp)); Button(onClick={vm.refresh()}){Text("Refresh now")} } }
        item { Text("Last success: ${s.lastSuccess?.let{Instant.ofEpochSecond(it)} ?: "Never"}") }
        item { Text("Background: ${s.bgEnabled}, every ${s.intervalHours}h") }
        item { Text("Notification permission: $notifPerm, Location permission: $locPerm") }
        item { Text("Diagnostics: lastFail=${s.lastFail?.let{Instant.ofEpochSecond(it)}}, err=${s.lastErr}, metaCached=${s.hasMeta}") }
        item { Row(verticalAlignment= androidx.compose.ui.Alignment.CenterVertically){Text("Notifications"); Switch(checked=s.notificationsEnabled,onCheckedChange=vm::setNotif)} }
        item { Row(verticalAlignment= androidx.compose.ui.Alignment.CenterVertically){Text("Background refresh"); Switch(checked=s.bgEnabled,onCheckedChange=vm::setBg)} }
        item { Row{ listOf(2,3).forEach{h-> FilterChip(selected=s.intervalHours==h,onClick={vm.setInterval(h)},label={Text("${h}h")}) } } }
        items(s.forecast?.rows ?: emptyList()) { r -> Text("${r.dayLabel}: ${r.high}/${r.low}° ${r.summary}") }
    }}
}

class RefreshWorker(ctx: Context, params: WorkerParameters): CoroutineWorker(ctx, params){ override suspend fun doWork(): Result { val repo=WeatherRepo(applicationContext); return if(repo.refreshNow().isSuccess) Result.success() else Result.retry() } }
fun schedule(ctx: Context, enabled:Boolean, hours:Int){ val wm=WorkManager.getInstance(ctx); if(!enabled){wm.cancelUniqueWork("periodic_weather"); return}; val req=PeriodicWorkRequestBuilder<RefreshWorker>(hours.toLong(),TimeUnit.HOURS).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()).build(); wm.enqueueUniquePeriodicWork("periodic_weather", ExistingPeriodicWorkPolicy.UPDATE, req) }

object WeatherNotifier { fun update(ctx:Context, c:CachedForecast){ val prefs=runCatching { kotlinx.coroutines.runBlocking { ctx.dataStore.data.first() } }.getOrNull(); if(prefs?.get(Keys.notif)==false) return
    val top=c.rows.firstOrNull(); val text= c.rows.take(3).joinToString(" • "){"${it.dayLabel} ${it.high}/${it.low} ${it.summary}"}
    val intent=Intent(ctx,MainActivity::class.java); val pi=android.app.PendingIntent.getActivity(ctx,0,intent,android.app.PendingIntent.FLAG_IMMUTABLE)
    val ri=Intent(ctx,RefreshActionReceiver::class.java); val rpi=android.app.PendingIntent.getBroadcast(ctx,1,ri,android.app.PendingIntent.FLAG_IMMUTABLE)
    val n=NotificationCompat.Builder(ctx,"forecast").setSmallIcon(android.R.drawable.ic_menu_compass).setContentTitle(top?.let{"${it.dayLabel} ${it.high}/${it.low}°"}?:"Forecast").setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setOngoing(true).setContentIntent(pi).addAction(0,"Refresh",rpi).build()
    NotificationManagerCompat.from(ctx).notify(1001,n)
}}
class RefreshActionReceiver: BroadcastReceiver(){ override fun onReceive(c: Context, i: Intent?) { WorkManager.getInstance(c).enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build()) } }
fun ensureChannel(ctx: Context){ if(Build.VERSION.SDK_INT>=26){ val nm=ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; nm.createNotificationChannel(NotificationChannel("forecast","Forecast",NotificationManager.IMPORTANCE_LOW)) } }
