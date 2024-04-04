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

fun SensorData.timezoneAsSeconds(): Int {
    val sign = timezone[0]
    val timezoneHours = timezone.substring(1, 3).toInt()
    val timezoneMinutes = timezone.substring(3, 5).toInt()

    return when (sign) {
        '+' -> timezoneHours * 3600 + timezoneMinutes * 60
        '-' -> -(timezoneHours * 3600 + timezoneMinutes * 60)
        else -> throw IllegalStateException("Sign(+,-) must be the first symbol in timezone!")
    }
}

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