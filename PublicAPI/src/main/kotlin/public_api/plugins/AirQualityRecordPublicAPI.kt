package public_api.plugins

import AirQualityService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureAirQualityRecordPublicAPI() {
    val airQualityService by inject<AirQualityService>()

    routing {
        route("/api") {
            route("/air-quality-records") {
                TODO()
            }
        }
    }
}