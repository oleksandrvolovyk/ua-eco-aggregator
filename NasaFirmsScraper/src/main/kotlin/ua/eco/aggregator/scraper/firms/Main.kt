package ua.eco.aggregator.scraper.firms

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
import ua.eco.aggregator.base.model.FireRecordDTO
import java.text.SimpleDateFormat
import java.util.*

val SCRAPING_API_URL: String = System.getenv("SCRAPING_API_URL")
val SCRAPING_API_KEY: String = System.getenv("SCRAPING_API_KEY")
val FIRMS_API_KEY: String = System.getenv("FIRMS_API_KEY")
const val FIRMS_API_BASE_URL = "https://firms.modaps.eosdis.nasa.gov/api/area/csv"

val POLLING_DELAY_IN_SECONDS = System.getenv("POLLING_DELAY_IN_SECONDS").toLong()
const val DELAY_BETWEEN_API_CALLS_MILLIS = 1_000L // 1 second

const val UKRAINE_AREA = "22,44,41,53" // Ukraine
const val DEFAULT_DAY_RANGE = 2 // 2 days

val SOURCES = listOf(
    "MODIS_NRT",
    "VIIRS_NOAA20_NRT",
    "VIIRS_NOAA21_NRT",
    "VIIRS_SNPP_NRT"
)

private fun buildFirmsApiUrls(
    baseUrl: String = FIRMS_API_BASE_URL,
    apiKey: String = FIRMS_API_KEY,
    sources: List<String> = SOURCES,
    area: String = UKRAINE_AREA,
    dayRange: Int = DEFAULT_DAY_RANGE,
    dateMillis: Long = System.currentTimeMillis()
): List<String> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val currentDate = dateFormat.format(Date(dateMillis))

    return buildList {
        sources.forEach { source ->
            add("$baseUrl/$apiKey/$source/$area/$dayRange/$currentDate")
        }
    }
}

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

    val firmsDateFormat = SimpleDateFormat("yyyy-MM-dd HHmm", Locale.getDefault())

    while (true) {
        val firmsApiUrls = buildFirmsApiUrls()

        val fireRecordDTOs = mutableListOf<FireRecordDTO>()

        firmsApiUrls.forEach { url ->
            val responseString: String = client.get(url).body()
            // CSV response format:
            // [0]latitude, [1]longitude, [2]brightness, [3]scan, [4]track, [5]acq_date, [6]acq_time, [7]satellite, [8]instrument,
            // [9]confidence, [10]version, [11]bright_t31, [12]frp, [13]daynight
            responseString.split("\n").drop(1).forEach { line ->
                val record = line.split(",")
                fireRecordDTOs.add(
                    FireRecordDTO(
                        latitude = record[0].toDouble(),
                        longitude = record[1].toDouble(),
                        timestamp = firmsDateFormat.parse("${record[5]} ${record[6].padStart(4, '0')}").time / 1000,
                        apiKey = SCRAPING_API_KEY,
                        metadata = "${record[7]} ${record[8]} ${record[10]}",
                        scan = record[3].toDouble(),
                        track = record[4].toDouble(),
                        confidence = record[9].toIntOrNull(),
                        fireRadiativePower = record[12].toDouble(),
                    )
                )
            }

            delay(DELAY_BETWEEN_API_CALLS_MILLIS)
        }

        println("Received ${fireRecordDTOs.size} records.")

        val response = client.post(SCRAPING_API_URL) {
            contentType(ContentType.Application.Json)
            setBody(fireRecordDTOs)
        }

        println("Sent ${fireRecordDTOs.size} records. API response status code: ${response.status}")
        println("Response: ${response.bodyAsText()}")

        delay(POLLING_DELAY_IN_SECONDS * 1000)
    }
}