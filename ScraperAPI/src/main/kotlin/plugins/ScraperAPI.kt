package plugins

import RecordService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.AggregatedRecordClasses
import model.AggregatedRecordDTO
import org.koin.java.KoinJavaComponent

fun Application.configureScraperAPI() {
    install(DoubleReceive)

    val recordServices = mutableListOf<RecordService<*, *>>()

    for (record in AggregatedRecordClasses) {
        recordServices.add(KoinJavaComponent.inject<RecordService<*, *>>(RecordService::class.java).value)
    }

    routing {
        for (record in AggregatedRecordClasses) {
            route("/${record.apiRoute}") {
                post {
                    val recordDTOs = call.receive<List<AggregatedRecordDTO>>()
                    val createdCount =
                        recordServices
                            .first { it.entityClassSimpleName == record.recordClass.simpleName }
                            .createMany(recordDTOs)
                }
            }
        }

        route("/air-quality-records") {
            // Post new air quality records
            post {
                val airQualityRecordDTOs = call.receive<List<AirQualityRecordDTO>>()
                val createdCount = airQualityService.create(airQualityRecordDTOs)
                call.respond(HttpStatusCode.Created, "Saved $createdCount records")
            }
        }

        route("/radiation-records") {
            // Post new radiation records
            post {
                val radiationRecordDTOs = call.receive<List<RadiationRecordDTO>>()
                val createdCount = radiationService.create(radiationRecordDTOs)
                call.respond(HttpStatusCode.Created, "Saved $createdCount records")
            }
        }
    }
}
