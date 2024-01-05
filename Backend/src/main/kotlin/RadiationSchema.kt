import kotlinx.coroutines.Dispatchers
import model.RadiationRecord
import model.RadiationRecordDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.java.KoinJavaComponent.inject
import java.time.Instant

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

    private suspend fun create(radiationRecordDTO: RadiationRecordDTO): Boolean {
        val scraper = scraperService.getByApiKey(radiationRecordDTO.apiKey)!!

        return RadiationRecords.insertIgnore {
            it[latitude] = radiationRecordDTO.latitude
            it[longitude] = radiationRecordDTO.longitude
            it[timestamp] = radiationRecordDTO.timestamp
            it[doseInNanoSievert] = radiationRecordDTO.doseInNanoSievert
            it[provider] = scraper.id
            it[metadata] = radiationRecordDTO.metadata
            it[createdAt] = Instant.now().epochSecond
        }.insertedCount != 0
    }

    suspend fun create(radiationRecordDTOs: List<RadiationRecordDTO>): Int = dbQuery {
        var addedCounter = 0

        radiationRecordDTOs.forEach { radiationRecordDTO ->
            if (create(radiationRecordDTO)) {
                addedCounter++
            }
        }

        return@dbQuery addedCounter
    }

    suspend fun readAll(): List<RadiationRecord> = dbQuery {
        RadiationRecords.selectAll().map { it.toRadiationRecord() }
    }

    suspend fun read(id: Int): RadiationRecord? = dbQuery {
        RadiationRecords.select { RadiationRecords.id eq id }.map { it.toRadiationRecord() }
    }.singleOrNull()

    suspend fun readLatestSubmittedRecordByProvider(providerId: Int): RadiationRecord? = dbQuery {
        RadiationRecords.select { RadiationRecords.provider eq providerId }
            .sortedByDescending { RadiationRecords.createdAt }
            .map { it.toRadiationRecord() }
    }.singleOrNull()

    suspend fun getTotalSubmittedRecordsByProvider(providerId: Int): Long = dbQuery {
        RadiationRecords.select { RadiationRecords.provider eq providerId }
            .count()
    }

    suspend fun delete(id: Int) = dbQuery {
        RadiationRecords.deleteWhere { RadiationRecords.id eq id }
    }

    private fun ResultRow.toRadiationRecord() = RadiationRecord(
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