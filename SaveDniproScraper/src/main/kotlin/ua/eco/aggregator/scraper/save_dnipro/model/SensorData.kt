package ua.eco.aggregator.scraper.save_dnipro.model

data class SensorData(
    val id: String,
    val cityName: String,
    val stationName: String,
    val localName: String,
    val timezone: String,
    val latitude: Double,
    val longitude: Double,
    val pollutants: List<Pollutant>,
    val platformName: String
)

data class Pollutant(
    val pol: String,
    val unit: String,
    val time: String,
    val value: Float,
    val averaging: String,
) {
    companion object {
        const val PM25 = "PM2.5"
        const val PM100 = "PM10"
        const val TEMPERATURE = "Temperature"
        const val HUMIDITY = "Humidity"
        const val PRESSURE = "Pressure"
    }
}