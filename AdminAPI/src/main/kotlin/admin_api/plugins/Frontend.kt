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
    //val airQualityService by inject<AirQualityService>()
    //val radiationService by inject<RadiationService>()

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
//
//                    data["totalSubmittedAirQualityRecords"] = airQualityService.getTotalSubmittedRecordsByProvider(id)
//                    data["totalSubmittedRadiationRecords"] = radiationService.getTotalSubmittedRecordsByProvider(id)
//
//                    airQualityService.readLatestSubmittedRecordByProvider(id)
//                        ?.let { data["latestSubmittedAirQualityRecord"] = it }
//
//                    radiationService.readLatestSubmittedRecordByProvider(id)
//                        ?.let { data["latestSubmittedRadiationRecord"] = it }

                    call.respond(ThymeleafContent("scraper", data))
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}