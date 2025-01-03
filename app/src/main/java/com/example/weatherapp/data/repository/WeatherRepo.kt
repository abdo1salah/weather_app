package com.example.weatherapp.data.repository

import android.content.Context
import com.example.weatherapp.data.local.DBHelper
import com.example.weatherapp.data.network.WeatherApi
import com.example.weatherapp.data.model.WeatherResponse
import com.example.weatherapp.data.network.ENDPOINT
import com.example.weatherapp.data.network.changeWeatherLocation
import com.example.weatherapp.data.network.getEndPoint
import com.example.weatherapp.location.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class WeatherRepo(context: Context) {
    private val db = DBHelper.getDBInstance(context)
    //val cashedData = db.weetherDao().getWeather()
    suspend fun getCashedData(): WeatherResponse {
        return db.weetherDao().getWeather()
    }
    val locationData = LocationData(context)

   /* suspend fun refreshData() {
        withContext(Dispatchers.IO) {
            val weatherData = WeatherApi.retrofitService.getData(ENDPOINT)
            db.weetherDao().insertWeatherData(weatherData)
        }
    }*/
   suspend fun refreshData() {
       withContext(Dispatchers.IO) {
           val locationData = locationData.getLastLocation().first()!!
           val weatherData = WeatherApi.retrofitService.getData(getEndPoint(locationData.latitude,locationData.longitude))
           db.weetherDao().insertWeatherData(weatherData)
       }
   }

    // Refresh weather data for a specific location
    suspend fun getWeatherDataForLocation(lat: String, lon: String): WeatherResponse {
        return withContext(Dispatchers.IO) {
            changeWeatherLocation(lat, lon)
            val locationWeatherData = WeatherApi.retrofitService.getData(ENDPOINT)
           // db.weetherDao().insertWeatherData(locationWeatherData)
            locationWeatherData
        }
    }
}