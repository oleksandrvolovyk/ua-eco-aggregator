package model

data class SensorData(
    val lat: Double,
    val long: Double,
    val time: String,
    val name: String,
    val value: Double
)