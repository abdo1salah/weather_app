package com.example.weatherapp.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.weatherapp.navigation.NavItem
import com.example.weatherapp.navigation.WeatherBottomNavigationBar
import com.example.weatherapp.navigation.WeatherNavHost
import com.example.weatherapp.util.WeatherViewModel

@Composable
fun MainScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    val navItemList = listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("Search", Icons.Default.Search),
        NavItem("Settings", Icons.Default.Settings)
    )

    val weatherViewModel: WeatherViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            WeatherBottomNavigationBar(
                navController = navController,
                navItemList = navItemList,
                currentDestination = currentDestination
            )
        }
    ) { innerPadding ->
        WeatherNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            weatherViewModel = weatherViewModel
        )
    }
}

@Composable
fun SettingsScreen() {
    Text(text = "Settings Screen", modifier = Modifier.fillMaxSize())}

