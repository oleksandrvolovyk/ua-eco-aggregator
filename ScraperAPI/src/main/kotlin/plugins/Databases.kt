package plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureDatabases() {
    install(DoubleReceive)

    val scraperService by inject<ScraperService>()
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

            // Post new air quality record
            post {
                val airQualityRecordDTO = call.receive<AirQualityRecordDTO>()
                val id = airQualityService.create(airQualityRecordDTO)
                call.respond(HttpStatusCode.Created, id)
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

            // Post new radiation record
            post {
                try {
                    val radiationRecordDTO = call.receive<RadiationRecordDTO>()
                    val id = radiationService.create(radiationRecordDTO)
                    call.respond(HttpStatusCode.Created, id)
                } catch (_: BadRequestException) {
                }

                try {
                    val radiationRecordDTOs = call.receive<List<RadiationRecordDTO>>()
                    radiationService.create(radiationRecordDTOs)
                    call.respond(HttpStatusCode.Created)
                } catch (_: BadRequestException) {
                }
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

        route("/scrapers") {
            // Get all scrapers
            get {
                val scrapers = scraperService.readAll()
                call.respond(HttpStatusCode.OK, scrapers)
            }

            // Read scraper by ID
            get("/{id}") {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                val scraper = scraperService.read(id)
                if (scraper != null) {
                    call.respond(HttpStatusCode.OK, scraper)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            // Create scraper
            post {
                val scraper = call.receive<ScraperDTO>()
                val id = scraperService.create(scraper.name, scraper.apiKey)
                call.respond(HttpStatusCode.Created, id)
            }

            // Update scraper
            put("/{id}") {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                val scraper = call.receive<ScraperDTO>()
                scraperService.update(id, scraper.name, scraper.apiKey)
                call.respond(HttpStatusCode.OK)
            }

            // Delete scraper
            delete("/{id}") {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                scraperService.delete(id)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
