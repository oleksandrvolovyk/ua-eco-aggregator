import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import model.AirQualityRecordDTO
import model.Pollutant
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
        install(HttpTimeout) {
            requestTimeoutMillis = 600_000
            connectTimeoutMillis = 600_000
            socketTimeoutMillis = 600_000
        }
    }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX")

    while (true) {
        val sensorDatas: List<SensorData> = client.get("https://api.saveecobot.com/output.json").body()

        val airQualityRecordDTOs = mutableListOf<AirQualityRecordDTO>()

        sensorDatas.forEach { sensorData ->
            val latitude = sensorData.latitude
            val longitude = sensorData.longitude

            val pm25record = sensorData.pollutants.firstOrNull { it.pol == Pollutant.PM25 }
            val pm100record = sensorData.pollutants.firstOrNull { it.pol == Pollutant.PM100 }

            if (pm25record != null && pm100record != null) {
                val date = dateFormat.parse(pm25record.time)
                val unixTimestamp = (date.time / 1000) - 7200 // Convert milliseconds to seconds and apply timezone

                val pm25 = pm25record.value
                val pm100 = pm100record.value

                airQualityRecordDTOs.add(
                    AirQualityRecordDTO(
                        latitude = latitude,
                        longitude = longitude,
                        timestamp = unixTimestamp,
                        pm10 = null,
                        pm25 = pm25,
                        pm100 = pm100,
                        apiKey = SCRAPING_API_KEY,
                        metadata = "SaveDniproScraper ${sensorData.cityName}, ${sensorData.stationName}"
                    )
                )
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