package ua.eco.aggregator.api.admin.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import ua.eco.aggregator.backend.ScraperService
import ua.eco.aggregator.base.model.ScraperDTO

fun Application.configureAPI() {
    val scraperService by inject<ScraperService>()

    routing {
        authenticate("auth-basic") {
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
                        val scraperDTO = call.receive<ScraperDTO>()
                        val id = scraperService.create(scraperDTO)
                        call.respond(HttpStatusCode.Created, id)
                    }

                    // Update scraper
                    put("/{id}") {
                        val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                        val scraperDTO = call.receive<ScraperDTO>()
                        scraperService.update(id, scraperDTO)
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
}