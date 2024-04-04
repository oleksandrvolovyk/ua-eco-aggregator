package ua.eco.aggregator.api.public.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import ua.eco.aggregator.backend.ScraperService
import ua.eco.aggregator.base.model.Scraper

@Serializable
data class PublicScraper(
    val id: Int,
    val name: String,
    val totalSubmittedRecords: Map<String, Long>
)

fun Scraper.toPublicScraper(totalSubmittedRecords: Map<String, Long>): PublicScraper =
    PublicScraper(
        id = this.id,
        name = this.name,
        totalSubmittedRecords = totalSubmittedRecords
    )

fun Application.configureScraperPublicAPI() {
    val scraperService by inject<ScraperService>()

    val recordServices = injectRecordServices()

    routing {
        route("/api") {
            route("/scrapers") {
                // Get all scrapers
                get {
                    val scrapers = scraperService.readAll().map { scraper ->
                        val totalSubmittedRecords = buildMap {
                            for (recordService in recordServices) {
                                this[recordService.recordsTableName] =
                                    recordService.getTotalSubmittedRecordsByProvider(scraper.id)
                            }
                        }

                        scraper.toPublicScraper(totalSubmittedRecords)
                    }
                    call.respond(HttpStatusCode.OK, scrapers)
                }

                // Read scraper by ID
                get("/{id}") {
                    val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                    val scraper = scraperService.read(id)
                    if (scraper != null) {
                        val totalSubmittedRecords = buildMap {
                            for (recordService in recordServices) {
                                this[recordService.recordsTableName] =
                                    recordService.getTotalSubmittedRecordsByProvider(scraper.id)
                            }
                        }

                        call.respond(HttpStatusCode.OK, scraper.toPublicScraper(totalSubmittedRecords))
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
}