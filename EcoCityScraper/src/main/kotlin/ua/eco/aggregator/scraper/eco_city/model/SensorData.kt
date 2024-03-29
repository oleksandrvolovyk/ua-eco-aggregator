package ua.eco.aggregator.scraper.eco_city.model

data class SensorData(
    val stationId: Int,
    val lat: Double,
    val long: Double,
    val time: String,
    val name: String,
    val value: String
)