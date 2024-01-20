package public_api.plugins

import RadiationService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRadiationRecordPublicAPI() {
    val radiationService by inject<RadiationService>()

    routing {
        route("/api") {
            route("/radiation-records") {
                TODO()
            }
        }
    }
}