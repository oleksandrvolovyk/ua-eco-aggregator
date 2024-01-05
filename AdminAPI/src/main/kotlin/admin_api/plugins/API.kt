package admin_api.plugins

import ScraperService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.ScraperDTO
import org.koin.ktor.ext.inject

fun Application.configureAPI() {
    val scraperService by inject<ScraperService>()

    routing {
        route("/api") {
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
}