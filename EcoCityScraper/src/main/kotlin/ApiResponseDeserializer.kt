import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import model.ApiResponse
import model.ApiResponseRecord
import model.Measurement
import model.Owner
import java.lang.reflect.Type

class ApiResponseDeserializer : JsonDeserializer<ApiResponse> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ApiResponse? {
        if (json == null) {
            return null
        }

        val result = mutableListOf<ApiResponseRecord>()

        if (json.isJsonObject) {
            val jsonObject = json.asJsonObject

            jsonObject.asMap().forEach { (_, value) ->
                val jsonMeasurementObject = value.asJsonObject

                if (jsonMeasurementObject.has("id")) { // It's a Measurement
                    result.add(
                        Measurement(
                            jsonMeasurementObject.get("name").asString,
                            jsonMeasurementObject.get("value").asString,
                            jsonMeasurementObject.get("time").asString
                        )
                    )
                } else if (jsonMeasurementObject.has("owner")) { // It's an Owner object
                    val owner = jsonMeasurementObject.get("owner").asJsonObject

                    result.add(Owner(owner.get("provider").asString))
                }
            }

            return ApiResponse(result)
        } else if (json.isJsonArray) {
            val jsonArray = json.asJsonArray

            jsonArray.forEach {
                val jsonMeasurementObject = it.asJsonObject

                if (jsonMeasurementObject.has("id")) { // It's a Measurement
                    result.add(
                        Measurement(
                            jsonMeasurementObject.get("name").asString,
                            jsonMeasurementObject.get("value").asString,
                            jsonMeasurementObject.get("time").asString
                        )
                    )
                } else if (jsonMeasurementObject.has("owner")) { // It's an Owner object
                    val owner = jsonMeasurementObject.get("owner").asJsonObject

                    result.add(Owner(owner.get("provider").asString))
                }
            }

            return ApiResponse(result)
        }

        return null
    }
}