import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import model.MeteoData
import java.lang.reflect.Type

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