package com.example.weatherapp.data.model

data class Forecastday(
    val astro: Astro,
    val date: String,
    val date_epoch: Int,
    val day: Day,
    //hour
    val hour: List<Hour>
)