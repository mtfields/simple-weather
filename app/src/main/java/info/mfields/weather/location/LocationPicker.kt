package info.mfields.weather.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import info.mfields.weather.SavedLocation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationPicker(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    @SuppressLint("MissingPermission")
    suspend fun currentCoarse(): SavedLocation? = suspendCancellableCoroutine { cont ->
        client.lastLocation.addOnSuccessListener { l -> cont.resume(l?.let { SavedLocation(it.latitude, it.longitude) }) }
            .addOnFailureListener { cont.resume(null) }
    }
}
