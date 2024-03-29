package ua.eco.aggregator.scraper.sensor_community.model

import com.google.gson.annotations.SerializedName

data class SensorData(
    val id: String,
    val timestamp: String,
    @SerializedName(value = "sensordatavalues") val sensorDataValues: List<Value>,
    val location: Location
) {
    data class Value(
        @SerializedName(value = "value_type") val valueType: String,
        val value: String
    )

    data class Location(
        val id: String,
        val altitude: Double,
        val longitude: Double,
        val latitude: Double
    )
}
