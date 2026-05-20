package info.mfields.weather

import android.content.Context
import info.mfields.weather.data.AppDataStore
import info.mfields.weather.domain.WeatherRepository
import info.mfields.weather.network.WeatherGovClient

class ServiceLocator(context: Context) {
    val store = AppDataStore(context)
    val repo = WeatherRepository(store, WeatherGovClient())
}
