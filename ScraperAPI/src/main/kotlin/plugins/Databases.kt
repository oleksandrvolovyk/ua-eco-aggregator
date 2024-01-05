package plugins

import AirQualityService
import RadiationService
import ScraperService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.AirQualityRecordDTO
import model.RadiationRecordDTO
import model.ScraperDTO
import org.koin.ktor.ext.inject

fun Application.configureDatabases() {
    install(DoubleReceive)

    val airQualityService by inject<AirQualityService>()
    val radiationService by inject<RadiationService>()

    routing {
        route("/air-quality-records") {
            // Get all air quality records
            get {
                val records = airQualityService.readAll()
                call.respond(HttpStatusCode.OK, records)
            }

            // Get air quality record by id
            get("/{id}") {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                val record = airQualityService.read(id)
                if (record != null) {
                    call.respond(HttpStatusCode.OK, record)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            // Post new air quality records
            post {
                val airQualityRecordDTOs = call.receive<List<AirQualityRecordDTO>>()
                val createdCount = airQualityService.create(airQualityRecordDTOs)
                call.respond(HttpStatusCode.Created, "Saved $createdCount records")
            }

            // Delete air quality record by id
            delete("/{id}") {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                airQualityService.delete(id)
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/radiation-records") {
            // Get all radiation records
            get {
                val records = radiationService.readAll()
                call.respond(HttpStatusCode.OK, records)
            }

            // Get radiation record by id
            get("/{id}") {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                val record = radiationService.read(id)
                if (record != null) {
                    call.respond(HttpStatusCode.OK, record)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            // Post new radiation records
            post {
                val radiationRecordDTOs = call.receive<List<RadiationRecordDTO>>()
                val createdCount = radiationService.create(radiationRecordDTOs)
                call.respond(HttpStatusCode.Created, "Saved $createdCount records")
            }

            // Delete radiation record
            delete("/{id}") {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                when (radiationService.delete(id)) {
                    0 -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
