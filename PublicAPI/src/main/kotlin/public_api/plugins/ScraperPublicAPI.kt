package public_api.plugins

import ScraperService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import model.Scraper
import org.koin.ktor.ext.inject

// Scraper without ApiKey
@Serializable
data class PublicScraper(val id: Int, val name: String)

fun Scraper.toPublicScraper(): PublicScraper = PublicScraper(id = this.id, name = this.name)

fun Application.configureScraperPublicAPI() {
    val scraperService by inject<ScraperService>()

    routing {
        route("/api") {
            route("/scrapers") {
                // Get all scrapers
                get {
                    val scrapers = scraperService.readAll().map { it.toPublicScraper() }
                    call.respond(HttpStatusCode.OK, scrapers)
                }

                // Read scraper by ID
                get("/{id}") {
                    val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                    val scraper = scraperService.read(id)?.toPublicScraper()
                    if (scraper != null) {
                        call.respond(HttpStatusCode.OK, scraper)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
}