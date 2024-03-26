package plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import model.*

fun Application.configureScraperAPI() {
    install(DoubleReceive)

    val recordServices = injectRecordServices()

    routing {
        for (recordData in AggregatedRecordClasses) {
            route("/${recordData.apiRoute}") {
                // Post new records
                post {
                    // Receive record DTOs dynamically
                    val recordDTOsString = call.receive<String>()

                    val recordDTOs: List<AggregatedRecordDTO> =
                        Json.decodeFromString(ListSerializer(RecordDTOSerializer), recordDTOsString)

                    // Find the appropriate service for the record type
                    val service = recordServices
                        .firstOrNull { it.entityClassSimpleName == recordData.recordClass.simpleName }

                    // If the service is found, invoke createMany with the specific DTOs
                    val createdCount = service?.createMany(recordDTOs as List<Nothing>) ?: 0

                    call.respond(HttpStatusCode.Created, "Saved $createdCount records")
                }
            }
        }
    }
}

object RecordDTOSerializer : JsonContentPolymorphicSerializer<AggregatedRecordDTO>(AggregatedRecordDTO::class) {
    private val recordServices = injectRecordServices()

    @OptIn(InternalSerializationApi::class)
    override fun selectDeserializer(element: JsonElement): KSerializer<out AggregatedRecordDTO> {
        for (recordService in recordServices) {
            if (recordService.entityDataProperties.any { it.name in element.jsonObject }) {
                return recordService.entityDTOClass.serializer()
            }
        }
        throw IllegalArgumentException("No deserializer found.")
    }
}