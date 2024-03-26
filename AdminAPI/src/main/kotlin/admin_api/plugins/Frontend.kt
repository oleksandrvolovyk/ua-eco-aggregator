package admin_api.plugins

import ScraperService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import org.koin.ktor.ext.inject

fun Application.configureFrontend() {
    val scraperService by inject<ScraperService>()
    val recordServices = injectRecordServices()

    routing {
        route("/scrapers") {
            get {
                call.respond(
                    ThymeleafContent(
                        "scrapers", mapOf(
                            "scrapers" to scraperService.readAll()
                        )
                    )
                )
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                val scraper = scraperService.read(id)
                if (scraper != null) {
                    val data = mutableMapOf<String, Any>()

                    data["scraper"] = scraper

                    val totalSubmittedRecordsData = buildMap {
                        for (recordService in recordServices) {
                            this[recordService.recordsTableName] =
                                recordService.getTotalSubmittedRecordsByProvider(scraper.id)
                        }
                    }

                    data["totalSubmittedRecords"] = totalSubmittedRecordsData

                    call.respond(ThymeleafContent("scraper", data))
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}