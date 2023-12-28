import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import model.*
import java.text.SimpleDateFormat

val BLOCK_LIST = listOf("ЛУН Місто Air")
const val REQUESTS_DELAY = 1000L // ms

fun main() = runBlocking {

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson {
                registerTypeAdapter(ApiResponse::class.java, ApiResponseDeserializer())
            }
        }
    }

    val publicData: PublicData =
        client.get("https://eco-city.org.ua/public.json?key=25092023&coords={%22south%22:19.034912633967924,%22west%22:-39.496555979508265,%22north%22:77.96474517354885,%22east%22:88.47219402049173}")
            .body()

    val stations = publicData.stations

    val sensorData = mutableListOf<SensorData>()

    run loop@ {
        stations.forEachIndexed { index, station ->
            if (index == 3) return@loop

            println("Requesting https://eco-city.org.ua/public.json?key=25092023&id=${station.id}&timeShift=0")
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
                    sensorData.add(
                        SensorData(
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
            println("${index + 1}/${stations.size}")
        }
    }

    client.close()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

    println(
        buildString {
            sensorData.forEach {
                val date = dateFormat.parse(it.time)
                val unixTimestamp = date.time / 1000 // Convert milliseconds to seconds
                append("TIMESTAMP: $unixTimestamp, LAT: ${it.lat}, LONG: ${it.long}, ${it.name} = ${it.value}\n")
            }
        }
    )
}