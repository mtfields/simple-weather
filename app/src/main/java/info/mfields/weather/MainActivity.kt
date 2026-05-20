package info.mfields.weather

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

val Context.store by preferencesDataStore("weather_store")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = WeatherRepository(applicationContext)
        setContent { App(repo) }
    }
}

@Serializable data class HourPeriod(val start:String,val temp:Int,val unit:String,val short:String)
@Serializable data class DaySummary(val day:String,val high:Int,val low:Int,val summary:String)
@Serializable data class Cache(val daily:List<DaySummary>,val updated:String)

class WeatherRepository(private val ctx: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val LAT=doublePreferencesKey("lat"); private val LON=doublePreferencesKey("lon")
    private val GRID=stringPreferencesKey("grid"); private val FORECAST_URL=stringPreferencesKey("forecast_url"); private val GRID_TS=longPreferencesKey("grid_ts")
    private val CACHE=stringPreferencesKey("cache"); private val LAST_OK=longPreferencesKey("last_ok"); private val LAST_FAIL=longPreferencesKey("last_fail"); private val LAST_ERR=stringPreferencesKey("last_err")
    private val BG=booleanPreferencesKey("bg_enabled"); private val NOTIF=booleanPreferencesKey("notif_enabled")
    val state: Flow<UiState> = ctx.store.data.map { p -> UiState(p[LAT],p[LON],p[CACHE]?.let{json.decodeFromString<Cache>(it)}?.daily?: emptyList(),p[LAST_OK],p[LAST_FAIL],p[LAST_ERR],p[BG]?:true,p[NOTIF]?:true,p[GRID],p[FORECAST_URL]) }
    suspend fun setLocation(lat:Double,lon:Double){ ctx.store.edit{it[LAT]=lat;it[LON]=lon;it.remove(GRID);it.remove(FORECAST_URL)} }
    suspend fun setBg(b:Boolean){ctx.store.edit{it[BG]=b}; schedulePeriodic(ctx,this)}
    suspend fun setNotif(b:Boolean){ctx.store.edit{it[NOTIF]=b}; NotificationPublisher.update(ctx,this)}
    suspend fun refresh(): Result<Unit> = runCatching {
        val p=ctx.store.data.first(); val lat=p[LAT]?:error("No location"); val lon=p[LON]?:error("No location")
        var forecastUrl=p[FORECAST_URL]; val stale=(System.currentTimeMillis()-(p[GRID_TS]?:0L))> TimeUnit.DAYS.toMillis(7)
        if (forecastUrl==null||stale){
            val pointResp=get("https://api.weather.gov/points/$lat,$lon")
            val fu=Regex("\"forecastHourly\"\\s*:\\s*\"([^\"]+)\"").find(pointResp)?.groupValues?.get(1)?.replace("\\/","/")?:error("No hourly URL")
            val grid=Regex("\"gridId\"\\s*:\\s*\"([^\"]+)\"").find(pointResp)?.groupValues?.get(1)?:"?"
            ctx.store.edit{it[FORECAST_URL]=fu;it[GRID]=grid;it[GRID_TS]=System.currentTimeMillis()}; forecastUrl=fu
        }
        val forecast=get(forecastUrl!!)
        val periods=parsePeriods(forecast)
        val daily=summarize(periods)
        ctx.store.edit{it[CACHE]=json.encodeToString(Cache(daily,Instant.now().toString()));it[LAST_OK]=System.currentTimeMillis();it.remove(LAST_ERR)}
        NotificationPublisher.update(ctx,this)
    }.onFailure {e-> runBlocking{ctx.store.edit{it[LAST_FAIL]=System.currentTimeMillis();it[LAST_ERR]=e.message?:"error"}} }
    private fun get(u:String):String{ val c=URL(u).openConnection() as HttpURLConnection;c.setRequestProperty("User-Agent","simple-weather/1.0 (contact: local@device)");return c.inputStream.bufferedReader().readText() }
    fun parsePeriods(j:String):List<HourPeriod>{
        val rg=Regex("\\{[^{}]*\"startTime\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"temperature\"\\s*:\\s*([0-9-]+)[^{}]*\"temperatureUnit\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"shortForecast\"\\s*:\\s*\"([^\"]+)\"")
        return rg.findAll(j).map{HourPeriod(it.groupValues[1],it.groupValues[2].toInt(),it.groupValues[3],it.groupValues[4])}.toList()
    }
    fun summarize(hours:List<HourPeriod>):List<DaySummary>{
        return hours.groupBy { ZonedDateTime.parse(it.start).toLocalDate() }.entries.sortedBy { it.key }.take(5).map { (d,vals)->
            DaySummary(d.dayOfWeek.name.take(3), vals.maxOf{it.temp}, vals.minOf{it.temp}, vals.groupingBy{it.short}.eachCount().maxBy{it.value}.key)
        }
    }
}

data class UiState(val lat:Double?,val lon:Double?,val forecast:List<DaySummary>,val lastOk:Long?,val lastFail:Long?,val err:String?,val bg:Boolean,val notif:Boolean,val grid:String?,val endpoint:String?)

@Composable fun App(repo: WeatherRepository){
    val ctx= LocalContext.current; val state by repo.state.collectAsState(initial = UiState(null,null, emptyList(),null,null,null,true,true,null,null))
    val scope = rememberCoroutineScope()
    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    Scaffold { pad-> LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)){
        item { Text("Saved location: ${state.lat ?: "unset"}, ${state.lon ?: "unset"}") }
        item { Button(onClick={ if (ContextCompat.checkSelfPermission(ctx,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            LocationServices.getFusedLocationProviderClient(ctx).lastLocation.addOnSuccessListener { l-> if(l!=null) scope.run { kotlinx.coroutines.launch{repo.setLocation(l.latitude,l.longitude)} } }
        } else locPerm.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }){Text("Use current location")} }
        item { Button(onClick={ scope.run { kotlinx.coroutines.launch{repo.refresh()} } }){Text("Refresh now")} }
        item { Row{ Text("Notifications"); Switch(checked=state.notif,onCheckedChange={v-> scope.run{kotlinx.coroutines.launch{repo.setNotif(v)}}}) } }
        item { Row{ Text("Background refresh"); Switch(checked=state.bg,onCheckedChange={v-> scope.run{kotlinx.coroutines.launch{repo.setBg(v)}}}) } }
        items(state.forecast){ d-> Text("${d.day}: ${d.high}/${d.low} ${d.summary}") }
        item { Text("Last success: ${state.lastOk?.let{Instant.ofEpochMilli(it)} ?: "never"}") }
        item { Text("Last fail: ${state.lastFail?.let{Instant.ofEpochMilli(it)} ?: "never"}") }
        item { Text("Error: ${state.err ?: "none"}") }
        item { Text("Grid: ${state.grid ?: "none"}") }
        item { Text("Endpoint cached: ${state.endpoint != null}") }
        item { Text("Notif permission: ${if(Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(ctx,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)"granted" else "missing"}") }
        item { Text("Location permission: ${if(ContextCompat.checkSelfPermission(ctx,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED)"granted" else "missing"}") }
    } }
    LaunchedEffect(state.bg){ schedulePeriodic(ctx,repo) }
}

suspend fun schedulePeriodic(ctx:Context, repo:WeatherRepository){
    val st=repo.state.first(); val wm=WorkManager.getInstance(ctx)
    if(!st.bg){ wm.cancelUniqueWork("periodic_weather"); return }
    val req=PeriodicWorkRequestBuilder<RefreshWorker>(3,TimeUnit.HOURS).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()).build()
    wm.enqueueUniquePeriodicWork("periodic_weather", ExistingPeriodicWorkPolicy.UPDATE, req)
}

class RefreshWorker(ctx: Context, params: WorkerParameters): CoroutineWorker(ctx,params){
    override suspend fun doWork(): Result { val r=WeatherRepository(applicationContext).refresh(); return if(r.isSuccess) Result.success() else Result.retry() }
}

object NotificationPublisher{
    fun update(ctx:Context, repo:WeatherRepository){
        val st= runBlocking { repo.state.first() }
        if(!st.notif) { (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1001); return }
        val nm=ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("forecast","Forecast",NotificationManager.IMPORTANCE_LOW))
        val text=st.forecast.take(3).joinToString(" · "){"${it.day} ${it.high}/${it.low} ${it.summary}"}.ifBlank { "No cached forecast" }
        val intent=PendingIntent.getActivity(ctx,0,Intent(ctx,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        val refresh=PendingIntent.getBroadcast(ctx,1,Intent(ctx,RefreshReceiver::class.java),PendingIntent.FLAG_IMMUTABLE)
        nm.notify(1001, NotificationCompat.Builder(ctx,"forecast").setSmallIcon(android.R.drawable.ic_menu_compass).setContentTitle("Local forecast").setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setOngoing(true).setContentIntent(intent).addAction(0,"Refresh",refresh).build())
    }
}

class RefreshReceiver: BroadcastReceiver(){ override fun onReceive(c:Context,i:Intent){ WorkManager.getInstance(c).enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build()) } }
