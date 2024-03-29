package ua.eco.aggregator.scraper.eco_zagroza.model

import com.google.gson.annotations.SerializedName

data class SensorData(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val layer: String,
    val markerIcon: String,
    @SerializedName(value = "indexValue") val doseInNanoSievert: Int
)