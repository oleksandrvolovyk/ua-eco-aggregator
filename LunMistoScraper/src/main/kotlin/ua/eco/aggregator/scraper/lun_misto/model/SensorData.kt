package ua.eco.aggregator.scraper.lun_misto.model

import com.google.gson.annotations.SerializedName

data class SensorData(
    val name: String,
    @SerializedName(value = "lat") val latitude: Double,
    @SerializedName(value = "lng") val longitude: Double,
    val city: String,
    val aqi: Double,
    @SerializedName(value = "avgPm10") val pm10: Double?,
    @SerializedName(value = "avgPm25") val pm25: Double,
    @SerializedName(value = "avgPm100") val pm100: Double,
    val updated: String
)