import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.runBlocking
import model.SensorData
import java.text.SimpleDateFormat

fun main() = runBlocking {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }
    }

    val sensorData: List<SensorData> =
        client.get("https://data.sensor.community/airrohr/v1/filter/country=UA").body()

    client.close()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

    println(
        buildString {
            sensorData.forEach {
                val date = dateFormat.parse(it.timestamp)
                val unixTimestamp = date.time / 1000 // Convert milliseconds to seconds
                append("TIMESTAMP: $unixTimestamp, LAT: ${it.location.latitude}, LONG: ${it.location.longitude}, values: ${it.sensorDataValues}\n")
            }
        }
    )
}