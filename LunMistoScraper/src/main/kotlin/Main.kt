import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import model.AirQualityRecordDTO
import model.SensorData
import java.text.SimpleDateFormat

val SCRAPING_API_URL = System.getenv("SCRAPING_API_URL")
val SCRAPING_API_KEY = System.getenv("SCRAPING_API_KEY")

val POLLING_DELAY_IN_SECONDS = System.getenv("POLLING_DELAY_IN_SECONDS").toLong()

fun main() = runBlocking {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }
    }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

    while (true) {
        val sensorDatas: List<SensorData> = client.get("https://misto.lun.ua/api/air/v1/public/data").body()

        val airQualityRecordDTOs = mutableListOf<AirQualityRecordDTO>()

        sensorDatas.forEach { sensorData ->
            val latitude = sensorData.station.coordinates.lat
            val longitude = sensorData.station.coordinates.lng

            sensorData.particles.forEach { particle ->
                if (particle.pm10 != null && particle.pm25 != null && particle.pm100 != null) {

                    val date = dateFormat.parse(particle.time)
                    val unixTimestamp = date.time / 1000 // Convert milliseconds to seconds

                    airQualityRecordDTOs.add(
                        AirQualityRecordDTO(
                            latitude = latitude,
                            longitude = longitude,
                            timestamp = unixTimestamp,
                            pm10 = particle.pm10.toFloat(),
                            pm25 = particle.pm25.toFloat(),
                            pm100 = particle.pm100.toFloat(),
                            apiKey = SCRAPING_API_KEY,
                            metadata = "LunMistoScraper ${sensorData.station.city}-${sensorData.station.name}"
                        )
                    )
                }
            }
        }

        println("Received ${airQualityRecordDTOs.size} records.")

        val response = client.post(SCRAPING_API_URL) {
            contentType(ContentType.Application.Json)
            setBody(airQualityRecordDTOs)
        }

        println("Sent ${airQualityRecordDTOs.size} records. API response status code: ${response.status}")
        println("Response: ${response.bodyAsText()}")

        delay(POLLING_DELAY_IN_SECONDS * 1000)
    }
}