package public_api.plugins

import AirQualityService
import RadiationService
import ScraperService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import model.Scraper
import org.koin.ktor.ext.inject

@Serializable
data class PublicScraper(
    val id: Int,
    val name: String,
    val totalSubmittedAirQualityRecords: Long,
    val totalSubmittedRadiationRecords: Long
)

fun Scraper.toPublicScraper(
    totalSubmittedAirQualityRecords: Long,
    totalSubmittedRadiationRecords: Long
): PublicScraper =
    PublicScraper(
        id = this.id,
        name = this.name,
        totalSubmittedAirQualityRecords = totalSubmittedAirQualityRecords,
        totalSubmittedRadiationRecords = totalSubmittedRadiationRecords
    )

fun Application.configureScraperPublicAPI() {
    val scraperService by inject<ScraperService>()
    val airQualityService by inject<AirQualityService>()
    val radiationService by inject<RadiationService>()

    routing {
        route("/api") {
            route("/scrapers") {
                // Get all scrapers
                get {
                    val scrapers = scraperService.readAll().map {
                        val totalSubmittedAirQualityRecords = airQualityService.getTotalSubmittedRecordsByProvider(it.id)
                        val totalSubmittedRadiationRecords = radiationService.getTotalSubmittedRecordsByProvider(it.id)

                        it.toPublicScraper(totalSubmittedAirQualityRecords, totalSubmittedRadiationRecords)
                    }
                    call.respond(HttpStatusCode.OK, scrapers)
                }

                // Read scraper by ID
                get("/{id}") {
                    val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                    val scraper = scraperService.read(id)
                    if (scraper != null) {

                        val totalSubmittedAirQualityRecords = airQualityService.getTotalSubmittedRecordsByProvider(id)
                        val totalSubmittedRadiationRecords = radiationService.getTotalSubmittedRecordsByProvider(id)

                        val publicScraper = scraper.toPublicScraper(
                            totalSubmittedAirQualityRecords, totalSubmittedRadiationRecords
                        )

                        call.respond(HttpStatusCode.OK, publicScraper)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
}