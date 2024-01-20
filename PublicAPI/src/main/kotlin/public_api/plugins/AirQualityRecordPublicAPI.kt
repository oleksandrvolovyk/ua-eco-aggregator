package public_api.plugins

import AirQualityService
import SortDirection
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureAirQualityRecordPublicAPI() {
    val airQualityService by inject<AirQualityService>()

    routing {
        route("/api") {
            route("/air-quality-records") {
                get {
                    // Filter records by providerId
                    val providerId = call.parameters["providerId"]?.toInt()

                    // Filter by time period (timestamp_start, timestamp_end)
                    val timestampStart = call.parameters["timestampStart"]?.toLong()
                    val timestampEnd = call.parameters["timestampEnd"]?.toLong()

                    // Sort by timestamp OR sort by pm25 or pm100
                    val sortField = when (call.parameters["sortField"]) {
                        "timestamp" -> AirQualityService.SortField.TIMESTAMP
                        "pm25" -> AirQualityService.SortField.PM25
                        "pm100" -> AirQualityService.SortField.PM100
                        else -> AirQualityService.SortField.TIMESTAMP
                    }

                    val sortDirection = when (call.parameters["sortDirection"]) {
                        "ascending" -> SortDirection.ASCENDING
                        "descending" -> SortDirection.DESCENDING
                        else -> SortDirection.ASCENDING
                    }

                    val page = call.parameters["page"]?.toLongOrNull() ?: 0L

                    if (timestampStart != null && timestampEnd == null) {
                        throw IllegalArgumentException("No timestampEnd provided!")
                    } else if (timestampStart == null && timestampEnd != null) {
                        throw IllegalArgumentException("No timestampStart provided!")
                    } else {
                        call.respond(
                            HttpStatusCode.OK,
                            airQualityService.readPaginated(
                                providerId = providerId,
                                timestampStart = timestampStart,
                                timestampEnd = timestampEnd,
                                sortField = sortField,
                                sortDirection = sortDirection,
                                page = page
                            )
                        )
                    }
                }
            }
        }
    }
}