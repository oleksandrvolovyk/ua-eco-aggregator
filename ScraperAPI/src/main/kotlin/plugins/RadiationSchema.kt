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
data class ExposedRadiationRecord(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val doseInNanoSievert: Int,
    val providerId: Int,
    val metadata: String,
    val createdAt: Long
)

@Serializable
data class RadiationRecordDTO(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val doseInNanoSievert: Int,
    val apiKey: String,
    val metadata: String
)

class RadiationService(database: Database) {

    val scraperService by inject<ScraperService>(ScraperService::class.java)

    object RadiationRecords : Table() {
        val id = integer("id").autoIncrement()
        val latitude = double("latitude")
        val longitude = double("longitude")
        val timestamp = long("timestamp")

        val doseInNanoSievert = integer("doseInNanoSievert")

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
            SchemaUtils.create(RadiationRecords)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(radiationRecordDTO: RadiationRecordDTO): Int = dbQuery {
        val scraper = scraperService.getByApiKey(radiationRecordDTO.apiKey)!!

        RadiationRecords.insert {
            it[latitude] = radiationRecordDTO.latitude
            it[longitude] = radiationRecordDTO.longitude
            it[timestamp] = radiationRecordDTO.timestamp
            it[doseInNanoSievert] = radiationRecordDTO.doseInNanoSievert
            it[provider] = scraper.id
            it[metadata] = radiationRecordDTO.metadata
            it[createdAt] = Instant.now().epochSecond
        }[RadiationRecords.id]
    }

    suspend fun create(radiationRecordDTOs: List<RadiationRecordDTO>) = dbQuery {
        radiationRecordDTOs.forEach { radiationRecordDTO ->
            create(radiationRecordDTO)
        }
    }

    suspend fun readAll(): List<ExposedRadiationRecord> = dbQuery {
        RadiationRecords.selectAll().map { it.toRadiationRecord() }
    }

    suspend fun read(id: Int): ExposedRadiationRecord? = dbQuery {
        RadiationRecords.select { RadiationRecords.id eq id }.map { it.toRadiationRecord() }
    }.singleOrNull()

    suspend fun delete(id: Int) = dbQuery {
        RadiationRecords.deleteWhere { RadiationRecords.id eq id }
    }

    private fun ResultRow.toRadiationRecord() = ExposedRadiationRecord(
        this[RadiationRecords.id],
        this[RadiationRecords.latitude],
        this[RadiationRecords.longitude],
        this[RadiationRecords.timestamp],
        this[RadiationRecords.doseInNanoSievert],
        this[RadiationRecords.provider],
        this[RadiationRecords.metadata],
        this[RadiationRecords.createdAt]
    )
}
