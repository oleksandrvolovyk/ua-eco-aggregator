package model

data class SensorData(
    val station: Station,
    val particles: List<Particle>
) {
    data class Station(
        val coordinates: Coordinates,
        val city: String,
        val name: String,
    ) {
        data class Coordinates(val lat: Double, val lng: Double)
    }

    data class Particle(
        val time: String,
        val pm10: Double?,
        val pm25: Double?,
        val pm100: Double?
    )
}