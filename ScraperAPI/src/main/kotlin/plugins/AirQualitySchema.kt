package plugins

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.java.KoinJavaComponent.inject
import java.time.Instant

@Serializable
data class ExposedAirQualityRecord(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val pm10: Float? = null,
    val pm25: Float,
    val pm100: Float,
    val providerId: Int,
    val metadata: String,
    val createdAt: Long
)

@Serializable
data class AirQualityRecordDTO(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val pm10: Float? = null,
    val pm25: Float,
    val pm100: Float,
    val apiKey: String,
    val metadata: String
)

class AirQualityService(database: Database) {

    val scraperService by inject<ScraperService>(ScraperService::class.java)

    object AirQualityRecords : Table() {
        val id = integer("id").autoIncrement()
        val latitude = double("latitude")
        val longitude = double("longitude")
        val timestamp = long("timestamp")

        val pm10 = float("pm10").nullable()
        val pm25 = float("pm25")
        val pm100 = float("pm100")

        val provider = reference("provider", ScraperService.Scrapers.id)
        val metadata = varchar("metadata", length = 500)
        val createdAt = long("createdAt")

        override val primaryKey = PrimaryKey(id)

        init {
            uniqueIndex(latitude, longitude, timestamp, provider)
        }
    }

    init {
        transaction(database) {
            SchemaUtils.create(AirQualityRecords)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private suspend fun create(airQualityRecordDTO: AirQualityRecordDTO): Boolean {
        val scraper = scraperService.getByApiKey(airQualityRecordDTO.apiKey)!!

        return AirQualityRecords.insertIgnore {
            it[latitude] = airQualityRecordDTO.latitude
            it[longitude] = airQualityRecordDTO.longitude
            it[timestamp] = airQualityRecordDTO.timestamp
            it[pm10] = airQualityRecordDTO.pm10
            it[pm25] = airQualityRecordDTO.pm25
            it[pm100] = airQualityRecordDTO.pm100
            it[provider] = scraper.id
            it[metadata] = airQualityRecordDTO.metadata
            it[createdAt] = Instant.now().epochSecond
        }.insertedCount != 0
    }

    suspend fun create(airQualityRecordDTOs: List<AirQualityRecordDTO>): Int = dbQuery {
        var addedCounter = 0

        airQualityRecordDTOs.forEach { airQualityRecordDTO ->
            if (create(airQualityRecordDTO)) {
                addedCounter++
            }
        }

        return@dbQuery addedCounter
    }

    suspend fun readAll(): List<ExposedAirQualityRecord> = dbQuery {
        AirQualityRecords.selectAll().map { it.toAirQualityRecord() }
    }

    suspend fun read(id: Int): ExposedAirQualityRecord? = dbQuery {
        AirQualityRecords.select { AirQualityRecords.id eq id }.map { it.toAirQualityRecord() }
    }.singleOrNull()

    suspend fun delete(id: Int) = dbQuery {
        AirQualityRecords.deleteWhere { AirQualityRecords.id eq id }
    }

    private fun ResultRow.toAirQualityRecord() = ExposedAirQualityRecord(
        this[AirQualityRecords.id],
        this[AirQualityRecords.latitude],
        this[AirQualityRecords.longitude],
        this[AirQualityRecords.timestamp],
        this[AirQualityRecords.pm10],
        this[AirQualityRecords.pm25],
        this[AirQualityRecords.pm100],
        this[AirQualityRecords.provider],
        this[AirQualityRecords.metadata],
        this[AirQualityRecords.createdAt]
    )
}