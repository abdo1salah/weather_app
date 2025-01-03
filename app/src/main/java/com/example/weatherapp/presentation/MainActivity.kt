package com.example.weatherapp.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver

import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.weatherapp.R
import com.example.weatherapp.data.repository.WeatherRepo
import com.example.weatherapp.permissionDenied
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.weatherapp.presentation.MainScreen
import com.example.weatherapp.presentation.theme.WeatherAppTheme
import com.example.weatherapp.util.WeatherViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : ComponentActivity() {
    private var isDialogShown = mutableStateOf(false)
    private val weatherViewModel: WeatherViewModel by viewModels()

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installSplashScreen()
        val handler = handlePermissionResponse()
        CreateChannel()

        setContent {
            // Handle permission dialog
            if (isDialogShown.value) {
                permissionDenied { isDialogShown.value = true }
            }
            WeatherAppTheme {
                weatherViewModel.refreshData()

                // Log fetched data


                val navController = rememberNavController()
                MainScreen(navController = navController)
                //val weatherViewModel : WeatherViewModel = viewModel()

                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {}
                // Check if alerts are available and send alert notification if so

            }

            weatherViewModel.casheddata?.let {
                Log.d("WeatherDataFetch", "Fetched data: ${it}")
                if (it.alerts?.alert?.isNotEmpty() == true) {
                    sendAlertNotification()
                }

                // Box layout
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        handler.launch(Manifest.permission.POST_NOTIFICATIONS)

                }
            }
        }

    }

    override fun onStart() {
        super.onStart()
        weatherViewModel.refreshData()
    }
    private fun scheduleDailyNotification() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 7) // 6 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1) // Schedule for the next day
        }
//        val calendar = Calendar.getInstance().apply {
//            timeInMillis = System.currentTimeMillis()
//            add(Calendar.MINUTE, 1) // Set for 5 minutes in the future
//        }

        Log.d("AlarmManager", "Setting alarm for: ${calendar.time}")

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )


        // HomeScreen(viewModel= weatherViewModel )
    }


    @SuppressLint("NewApi")
    private fun handlePermissionResponse(): ActivityResultLauncher<String> {
        val launcher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    scheduleDailyNotification()
                } else {
                    isDialogShown.value = true

                }
            }
        return launcher
    }

    @SuppressLint("NewApi")
    private fun CreateChannel() {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("1", "Daily Notification", importance).apply {
            description = "Displays Daily Notification About today's weather"
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    private fun sendAlertNotification() {

        val alertHeadLine = weatherViewModel.casheddata?.alerts?.alert?.get(0)?.headline
        val alertEvent = weatherViewModel.casheddata?.alerts?.alert?.get(0)?.event
        val alertDescription = weatherViewModel.casheddata?.alerts?.alert?.get(0)?.severity
        val alertArea = weatherViewModel.casheddata?.alerts?.alert?.get(0)?.areas

        val weatherDescription = "${alertArea} , ${alertEvent} , ${alertDescription} "
        val intent = Intent(this, MainActivity::class.java) // Open MainActivity on notification tap
        val pendingIntent =
            PendingIntent.getActivity(this, 101, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "1")
            .setSmallIcon(R.drawable.sun)
            .setContentTitle("${alertHeadLine}")
            .setContentText(weatherDescription)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(99, notification)
    }

    class NotificationReceiver : BroadcastReceiver() {
        companion object {
            private const val REQUEST_CODE_MAIN_ACTIVITY = 101
        }

        override fun onReceive(context: Context, intent: Intent) {
            Log.d("NotificationReceiver", "onReceive: alarm Triggered")
            val weatherRepo = WeatherRepo(context)
            val sharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            val tempUnit = sharedPreferences.getString("Temperature Unit", "Celsius (°C)")
            CoroutineScope(Dispatchers.IO).launch {
                val weatherData = weatherRepo.getCashedData()
                withContext(Dispatchers.Main) {
                    // Use the weather data to create the notification
                    val condition = weatherData.current?.condition?.text
                    val location = weatherData.location?.name
                    val region = weatherData.location?.region
                    val maxtempC =
                        if (tempUnit == "Celsius (°C)") weatherData.forecast.forecastday.get(0).day
                            .maxtemp_c.toInt() else weatherData.forecast.forecastday.get(0)
                            .day.maxtemp_f.toInt()
                    val mintempC =
                        if (tempUnit == "Celsius (°C)") weatherData.forecast.forecastday.get(0)
                            .day.mintemp_c.toInt() else weatherData.forecast.forecastday.get(0)
                            .day.mintemp_f.toInt()
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context.applicationContext,
                        REQUEST_CODE_MAIN_ACTIVITY,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    val weatherDescription =
                        " ${region ?: "N/A"},  ${location ?: "N/A"} highs to ${maxtempC ?: "N/A"}${if (tempUnit == "Celsius (°C)") "C" else "F" } and lows to ${mintempC ?: "N/A"}${if (tempUnit == "Celsius (°C)") "C" else "F" } ,  ${condition ?: "N/A"}"

                    val builder = NotificationCompat.Builder(context, "weather_alerts")
                        .setSmallIcon(R.drawable.sun)
                        .setContentTitle("Today's Weather")
                        .setContentText(weatherDescription)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)

                    // Create the notification channel (required for Android 8.0 and above)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel = NotificationChannel(
                            "weather_alerts",
                            "Weather Alerts",
                            NotificationManager.IMPORTANCE_HIGH
                        )
                        val notificationManager =
                            context.getSystemService(NotificationManager::class.java)
                        notificationManager?.createNotificationChannel(channel)
                    }

                    // Show the notification
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(1, builder.build())
                }
            }
        }
    }
}
