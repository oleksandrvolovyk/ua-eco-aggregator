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

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

    while (true) {
        val sensorDatas: List<SensorData> =
            client.get("https://data.sensor.community/airrohr/v1/filter/country=UA").body()

        val airQualityRecordDTOs = mutableListOf<AirQualityRecordDTO>()

        sensorDatas.forEach { sensorData ->
            val date = dateFormat.parse(sensorData.timestamp)
            val unixTimestamp = date.time / 1000 // Convert milliseconds to seconds

            val pm10 =
                sensorData.sensorDataValues.firstOrNull { value -> value.valueType == "P0" }?.value?.toFloatOrNull()
            val pm25 =
                sensorData.sensorDataValues.firstOrNull { value -> value.valueType == "P2" }?.value?.toFloatOrNull()
            val pm100 =
                sensorData.sensorDataValues.firstOrNull { value -> value.valueType == "P1" }?.value?.toFloatOrNull()

            if (pm25 != null && pm100 != null) {
                airQualityRecordDTOs.add(
                    AirQualityRecordDTO(
                        latitude = sensorData.location.latitude,
                        longitude = sensorData.location.longitude,
                        timestamp = unixTimestamp,
                        pm10 = pm10,
                        pm25 = pm25,
                        pm100 = pm100,
                        apiKey = SCRAPING_API_KEY,
                        metadata = "SensorCommunityScraper"
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