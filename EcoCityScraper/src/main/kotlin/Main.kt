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
import model.*
import java.text.SimpleDateFormat

val BLOCK_LIST = listOf("ЛУН Місто Air")
const val REQUESTS_DELAY = 100L // ms

val SCRAPING_API_URL = System.getenv("SCRAPING_API_URL")
val SCRAPING_API_KEY = System.getenv("SCRAPING_API_KEY")

val POLLING_DELAY_IN_SECONDS = System.getenv("POLLING_DELAY_IN_SECONDS").toLong()

fun main() = runBlocking {

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson {
                registerTypeAdapter(ApiResponse::class.java, ApiResponseDeserializer())
            }
        }
    }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

    while (true) {
        val publicData: PublicData =
            client.get("https://eco-city.org.ua/public.json?key=25092023&coords={%22south%22:19.034912633967924,%22west%22:-39.496555979508265,%22north%22:77.96474517354885,%22east%22:88.47219402049173}")
                .body()

        val stations = publicData.stations

        val sensorDatas = mutableListOf<SensorData>()
        stations.forEachIndexed { index, station ->
            val response: ApiResponse? =
                client.get("https://eco-city.org.ua/public.json?key=25092023&id=${station.id}&timeShift=0").body()

            lateinit var owner: String
            val measurements = mutableListOf<Measurement>()

            response?.records?.forEach {
                when (it) {
                    is Owner -> {
                        owner = it.provider
                    }

                    is Measurement -> {
                        measurements.add(it)
                    }
                }
            }

            if (owner !in BLOCK_LIST) {
                measurements.forEach { measurement ->
                    sensorDatas.add(
                        SensorData(
                            stationId = station.id,
                            lat = station.latitude,
                            long = station.longitude,
                            time = measurement.time,
                            name = measurement.name,
                            value = measurement.value
                        )
                    )
                }
            }

            delay(REQUESTS_DELAY)
            if ((index + 1) % 10 == 0) {
                println("${index + 1}/${stations.size}")
            }
        }

        val airQualityRecordDTOs = mutableListOf<AirQualityRecordDTO>()

        sensorDatas.groupBy { it.stationId }.forEach { (stationId, stationSensorDatas) ->
            val pm10 = stationSensorDatas.firstOrNull { it.name == "PM1.0" }?.value?.toFloatOrNull()
            val pm25 = stationSensorDatas.firstOrNull { it.name == "PM2.5" }?.value?.toFloatOrNull()
            val pm25record = stationSensorDatas.firstOrNull {it.name == "PM2.5"}
            val pm100 = stationSensorDatas.firstOrNull { it.name == "PM10.0" }?.value?.toFloatOrNull()

            if (pm25record != null && pm25 != null && pm100 != null) {
                val date = dateFormat.parse(pm25record.time)
                val unixTimestamp = date.time / 1000 // Convert milliseconds to seconds

                airQualityRecordDTOs.add(
                    AirQualityRecordDTO(
                        latitude = pm25record.lat,
                        longitude = pm25record.long,
                        timestamp = unixTimestamp,
                        pm10 = pm10,
                        pm25 = pm25,
                        pm100 = pm100,
                        apiKey = SCRAPING_API_KEY,
                        metadata = "EcoCityScraper Station#${stationId}"
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

        delay(POLLING_DELAY_IN_SECONDS)
    }
}