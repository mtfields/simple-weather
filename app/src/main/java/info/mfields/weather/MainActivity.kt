package info.mfields.weather

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val vm by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { App(vm) } }
}

@Composable fun App(vm: MainViewModel) {
    val state by vm.state.collectAsState()
    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("Simple Weather") }) }) { p ->
            LazyColumn(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Saved location: ${state.location?.latitude}, ${state.location?.longitude}") }
                item { Button(onClick = { locPerm.launch(Manifest.permission.ACCESS_COARSE_LOCATION); vm.useCurrentLocation() }) { Text("Use current location") } }
                item { Button(onClick = vm::refreshNow) { Text("Refresh now") } }
                item { Text("Last success: ${state.lastSuccess}") }
                item { Text("Last failure: ${state.lastFail}") }
                item { Text("Error: ${state.lastError ?: "none"}") }
                items(state.forecast?.daily ?: emptyList()) { d -> Text("${d.dayLabel}: ${d.high}/${d.low} ${d.summary}") }
                item { Row { Text("Notifications"); Switch(state.notifEnabled, { vm.toggleNotif(it); if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) }) } }
                item { Row { Text("Background refresh"); Switch(state.bgEnabled, vm::toggleBg) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf(2,3,4).forEach { h -> FilterChip(selected = state.intervalHours==h, onClick={vm.setInterval(h)}, label={Text("${h}h")}) } } }
                item { Text("Diagnostics: points=${state.metadata?.pointsUrl} hourly=${state.metadata?.hourlyUrl} updated=${state.metadata?.updatedAt}") }
                item { Text("Permission notif=${vm.hasNotificationPermission()} coarseLoc=${vm.hasLocationPermission()} bg=${state.bgEnabled}") }
            }
        }
    }
}
