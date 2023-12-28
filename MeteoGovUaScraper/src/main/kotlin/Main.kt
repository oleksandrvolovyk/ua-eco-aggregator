import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import model.FullMeteoData
import model.MeteoData
import model.toFullMeteoData
import java.lang.reflect.Type
import java.text.SimpleDateFormat

fun main(): Unit = runBlocking {

    val client = HttpClient(CIO)
    val gson = GsonBuilder()
        .registerTypeAdapter(MeteoData::class.java, MeteoDataDeserializer())
        .create()

    val responseString = client.get("https://www.meteo.gov.ua/_/m/radioday.js").bodyAsText()

    client.close()

    val typeToken = object : TypeToken<Map<Int, MeteoData?>>() {}

    val meteoDatas = gson.fromJson(responseString, typeToken)

    val fullMeteodatas = mutableListOf<FullMeteoData>()

    meteoDatas.forEach { entry ->
        entry.value?.let { fullMeteodatas.add(it.toFullMeteoData(entry.key)) }
    }

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")

    println(
        buildString {
            fullMeteodatas.forEach {
                val date = dateFormat.parse("${it.date} ${it.time}")
                val unixTimestamp = date.time / 1000 // Convert milliseconds to seconds
                append("TIMESTAMP: ${unixTimestamp}, LAT: ${it.location.first}, LONG: ${it.location.second}, dose - ${it.doseInNanoSievert} nanoSievert/hour")
                append("\n")
            }
        }
    )
}

class MeteoDataDeserializer : JsonDeserializer<MeteoData> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): MeteoData? {
        if (json == null || !json.isJsonObject) {
            return null
        }

        val jsonObject = json.asJsonObject

        val date = jsonObject.get("CD")?.asString ?: ""
        val time = jsonObject.get("CH")?.asString ?: ""
        val doseInMicroRoentgen = jsonObject.get("VR")?.asInt ?: 0
        val doseInNanoSievert = jsonObject.get("VZ")?.asInt ?: 0

        return MeteoData(date, time, doseInMicroRoentgen, doseInNanoSievert)
    }
}