package public_api.plugins

import ScraperService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureScraperPublicAPI() {
    val scraperService by inject<ScraperService>()

    routing {
        route("/api") {
            route("/scrapers") {
                TODO()
            }
        }
    }
}