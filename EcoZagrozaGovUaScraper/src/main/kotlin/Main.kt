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
import model.RadiationRecordDTO
import model.SensorData

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

    val timestamp = System.currentTimeMillis() / 1000

    while (true) {
        val sensorDatas: List<SensorData> = client
            .get("https://sensor.ecozagroza.gov.ua/api/sensors?layer=radiation")
            .body()

        val radiationRecordDTOs = mutableListOf<RadiationRecordDTO>()

        sensorDatas.forEach { sensorData ->
            radiationRecordDTOs.add(
                RadiationRecordDTO(
                    latitude = sensorData.latitude,
                    longitude = sensorData.longitude,
                    timestamp = timestamp,
                    doseInNanoSievert = sensorData.doseInNanoSievert,
                    apiKey = SCRAPING_API_KEY,
                    metadata = "ecozagroza.gov.ua, id: ${sensorData.id}"
                )
            )
        }

        println("Received ${radiationRecordDTOs.size} records.")

        val response = client.post(SCRAPING_API_URL) {
            contentType(ContentType.Application.Json)
            setBody(radiationRecordDTOs)
        }

        println("Sent ${radiationRecordDTOs.size} records. API response status code: ${response.status}")
        println("Response: ${response.bodyAsText()}")

        delay(POLLING_DELAY_IN_SECONDS * 1000)
    }
}