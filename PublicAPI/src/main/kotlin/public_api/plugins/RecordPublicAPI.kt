package public_api.plugins

import SortDirection
import SortFieldName
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.AggregatedRecordClasses

const val SECONDS_IN_DAY = 86_400L

fun Application.configureRecordPublicAPI() {
    val recordServices = injectRecordServices()

    routing {
        route("/api") {
            for (recordType in AggregatedRecordClasses) {
                route("/${recordType.apiRoute}") {
                    get {
                        // Filter records by providerId
                        val providerId = call.parameters["providerId"]?.toInt()

                        // Filter by time period (timestamp_start, timestamp_end)
                        val timestampStart = call.parameters["timestampStart"]?.toLong()
                        val timestampEnd = call.parameters["timestampEnd"]?.toLong()

                        // Filter by location (latitude, longitude)
                        val latitude = call.parameters["latitude"]?.toDouble()
                        val longitude = call.parameters["longitude"]?.toDouble()

                        // Sort by timestamp OR entity data fields
                        val sortFieldName = call.parameters["sortField"] ?: SortFieldName.TIMESTAMP

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
                        } else if (latitude != null && longitude == null) {
                            throw IllegalArgumentException("No longitude provided!")
                        } else if (latitude == null && longitude != null) {
                            throw IllegalArgumentException("No latitude provided!")
                        } else {
                            call.respond(
                                HttpStatusCode.OK,
                                recordServices
                                    .first { it.entityClassSimpleName == recordType.recordClass.simpleName }
                                    .readPaginated(
                                        providerId = providerId,
                                        timestampStart = timestampStart,
                                        timestampEnd = timestampEnd,
                                        latitude = latitude,
                                        longitude = longitude,
                                        sortFieldName = sortFieldName,
                                        sortDirection = sortDirection,
                                        page = page
                                    )
                            )
                        }
                    }

                    get("/latest") {
                        // Optional parameter to get latest records at given timestamp (to get historical data)
                        val at = call.parameters["at"]?.toLong()
                        // Optional paramater to set max age for records, one day by default
                        val maxAge = call.parameters["maxAge"]?.toLong() ?: SECONDS_IN_DAY

                        call.respond(
                            HttpStatusCode.OK,
                            recordServices
                                .first { it.entityClassSimpleName == recordType.recordClass.simpleName }
                                .readLatestSubmittedRecordsWithDistinctLocations(at, maxAge)
                        )
                    }

                    get("/{id}") {
                        val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                        val record = recordServices
                            .first { it.entityClassSimpleName == recordType.recordClass.simpleName }
                            .read(id)
                        if (record != null) {
                            call.respond(HttpStatusCode.OK, record)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
            }
        }
    }
}