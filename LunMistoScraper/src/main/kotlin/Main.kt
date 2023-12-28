import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.runBlocking
import model.SensorData
import java.text.SimpleDateFormat

fun main(args: Array<String>) = runBlocking {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }
    }

    val sensorData: List<SensorData> =
        client.get("https://misto.lun.ua/api/air/v1/public/data").body()

    client.close()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

    println(
        buildString {
            sensorData.forEach {
                append("LAT: ${it.station.coordinates.lat}, LONG: ${it.station.coordinates.lng}\n")
                it.particles.forEach { particle ->
                    val date = dateFormat.parse(particle.time)
                    val unixTimestamp = date.time / 1000 // Convert milliseconds to seconds
                    append("\tTIMESTAMP: $unixTimestamp, PM1.0: ${particle.pm10}, PM2.5: ${particle.pm25}, PM10.0: ${particle.pm100}\n")
                }
            }
        }
    )
}