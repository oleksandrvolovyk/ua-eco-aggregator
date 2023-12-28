package model

data class FullMeteoData(
    val stationId: Int,
    val stationName: String,
    val location: Coordinates, // Lat, Long
    val date: String,
    val time: String,
    val doseInMicroRoentgen: Int,
    val doseInNanoSievert: Int
)