package info.mfields.weather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import info.mfields.weather.ui.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MaterialTheme { AppScreen() } } }
}

@Composable
fun AppScreen(vm: MainViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.onLocationPermissionResult(it) }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Saved location: ${s.location ?: "Not set"}")
        Row {
            Button(onClick = { vm.refreshNow() }) { Text("Refresh now") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val granted = ContextCompat.checkSelfPermission(LocalContext.current, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (granted) vm.onLocationPermissionResult(true) else launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }) { Text("Use current location") }
        }
        Text("Last success: ${s.lastSuccess}")
        Text("Last fail: ${s.lastFail ?: "-"}")
        Text("Error: ${s.lastError ?: "none"}")
        Row { Text("Notifications"); Switch(checked = s.notificationsEnabled, onCheckedChange = vm::setNotificationsEnabled) }
        Row { Text("Background"); Switch(checked = s.backgroundEnabled, onCheckedChange = vm::setBackgroundEnabled) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Interval")
            FilterChip(selected = s.refreshIntervalHours == 2, onClick = { vm.setRefreshIntervalHours(2) }, label = { Text("2h") })
            FilterChip(selected = s.refreshIntervalHours == 3, onClick = { vm.setRefreshIntervalHours(3) }, label = { Text("3h") })
        }
        Text("Diagnostics: ${s.diagnostics}")
        LazyColumn { items(s.days) { d -> Text("${d.day}: ${d.high}/${d.low} ${d.summary}") } }
    }
}
