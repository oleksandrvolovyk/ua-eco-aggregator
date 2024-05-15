package ua.eco.aggregator.scraper.lun_misto

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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import ua.eco.aggregator.base.model.AirQualityRecordDTO
import ua.eco.aggregator.scraper.lun_misto.model.SensorData
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

    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

    while (coroutineContext.isActive) {
        val sensorDatas: List<SensorData> = client.get("https://misto.lun.ua/api/v1/air/stations").body()

        val airQualityRecordDTOs = sensorDatas.map { sensorData ->
            AirQualityRecordDTO(
                latitude = sensorData.latitude,
                longitude = sensorData.longitude,
                timestamp = dateFormat.parse(sensorData.updated).time / 1000,
                pm10 = sensorData.pm10?.toFloat(),
                pm25 = sensorData.pm25.toFloat(),
                pm100 = sensorData.pm100.toFloat(),
                apiKey = SCRAPING_API_KEY,
                metadata = "LunMistoScraper ${sensorData.city}-${sensorData.name}"
            )
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