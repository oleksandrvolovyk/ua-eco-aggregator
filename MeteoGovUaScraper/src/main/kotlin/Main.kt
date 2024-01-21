import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import model.FullMeteoData
import model.MeteoData
import model.RadiationRecordDTO
import model.toFullMeteoData
import java.text.SimpleDateFormat

val SCRAPING_API_URL = System.getenv("SCRAPING_API_URL")
val SCRAPING_API_KEY = System.getenv("SCRAPING_API_KEY")

val POLLING_DELAY_IN_SECONDS = System.getenv("POLLING_DELAY_IN_SECONDS").toLong()

fun main(): Unit = runBlocking {

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
    }

    val gson = GsonBuilder()
        .registerTypeAdapter(MeteoData::class.java, MeteoDataDeserializer())
        .create()

    val typeToken = object : TypeToken<Map<Int, MeteoData?>>() {}

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")

    while (true) {
        val responseString = client.get("https://www.meteo.gov.ua/_/m/radioday.js").bodyAsText()

        val meteoDatas = gson.fromJson(responseString, typeToken)

        val fullMeteodatas = mutableListOf<FullMeteoData>()

        meteoDatas.forEach { entry ->
            entry.value?.let { fullMeteodatas.add(it.toFullMeteoData(entry.key)) }
        }

        val radiationRecordDTOs = List(fullMeteodatas.size) { index ->
            val fullMeteodata = fullMeteodatas[index]

            val date = dateFormat.parse("${fullMeteodata.date} ${fullMeteodata.time}")
            val unixTimestamp = date.time / 1000 // Convert milliseconds to seconds

            RadiationRecordDTO(
                latitude = fullMeteodata.location.first,
                longitude = fullMeteodata.location.second,
                timestamp = unixTimestamp,
                doseInNanoSievert = fullMeteodata.doseInNanoSievert,
                apiKey = SCRAPING_API_KEY,
                metadata = "MeteoGovUaScraper $index"
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