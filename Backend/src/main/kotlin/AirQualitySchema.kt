import kotlinx.coroutines.Dispatchers
import model.AirQualityRecord
import model.AirQualityRecordDTO
import model.PaginatedData
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal
import java.time.Instant

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

class AirQualityService(database: Database, private val pageSize: Int) {

    enum class SortField {
        TIMESTAMP,
        PM25,
        PM100
    }

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

        return try {
            AirQualityRecords.insert {
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
        } catch (e: ExposedSQLException) {
            false
        }
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

    suspend fun readAll(): List<AirQualityRecord> = dbQuery {
        AirQualityRecords.selectAll().map { it.toAirQualityRecord() }
    }

    suspend fun readPaginated(
        providerId: Int?,
        timestampStart: Long?,
        timestampEnd: Long?,
        latitude: Double?,
        longitude: Double?,
        sortField: SortField,
        sortDirection: SortDirection,
        page: Long
    ): PaginatedData<AirQualityRecord> = dbQuery {
        var query = AirQualityRecords.selectAll()
        // 1. Apply filters
        // 1.1 Filter by providerId
        if (providerId != null) {
            query = query.andWhere { AirQualityRecords.provider eq providerId }
        }

        // 1.2 Filter by time period
        if (timestampStart != null && timestampEnd != null) {
            query =
                query.andWhere { (AirQualityRecords.timestamp greaterEq timestampStart) and (AirQualityRecords.timestamp lessEq timestampEnd) }
        }

        // 1.3 Filter by location
        if (latitude != null && longitude != null) {
            query =
                query.andWhere { (AirQualityRecords.latitude eq latitude) and (AirQualityRecords.longitude eq longitude) }
        }

        // 2. Apply sorting
        query = when (sortField) {
            SortField.TIMESTAMP -> query.orderBy(
                AirQualityRecords.timestamp to (
                        if (sortDirection == SortDirection.ASCENDING)
                            SortOrder.ASC
                        else
                            SortOrder.DESC
                        )
            )

            SortField.PM25 -> query.orderBy(
                AirQualityRecords.pm25 to (
                        if (sortDirection == SortDirection.ASCENDING)
                            SortOrder.ASC
                        else
                            SortOrder.DESC
                        )
            )

            SortField.PM100 -> query.orderBy(
                AirQualityRecords.pm100 to (
                        if (sortDirection == SortDirection.ASCENDING)
                            SortOrder.ASC
                        else
                            SortOrder.DESC
                        )
            )
        }

        val totalAirQualityRecords = query.count()

        // 3. Apply paging
        val data = query
            .limit(pageSize, page * pageSize)
            .map { it.toAirQualityRecord() }

        return@dbQuery PaginatedData(
            page = page,
            maxPageNumber = totalAirQualityRecords / pageSize,
            itemsPerPage = pageSize,
            totalItemsCount = totalAirQualityRecords,
            data = data
        )
    }

    suspend fun readLatestSubmittedRecordsWithDistinctLocations(
        at: Long? = null,
        maxAge: Long? = null
    ): List<AirQualityRecord> = newSuspendedTransaction {
        val result = arrayListOf<AirQualityRecord>()

        val sqlWhereTimestamp =
            if (at != null && maxAge != null)
                "WHERE timestamp < $at AND timestamp > ${at - maxAge}"
            else if (at != null)
                "WHERE timestamp < $at"
            else if (maxAge != null)
                "WHERE timestamp > ${System.currentTimeMillis() / 1000 - maxAge}"
            else ""

        val sqlStatement =
            """
                SELECT t1.*
                FROM airqualityrecords t1
                JOIN (
                    SELECT latitude, longitude, MAX(timestamp) AS latest_timestamp
                    FROM airqualityrecords
                    $sqlWhereTimestamp
                    GROUP BY latitude, longitude
                ) t2 ON t1.latitude = t2.latitude AND t1.longitude = t2.longitude AND t1.timestamp = t2.latest_timestamp
            """.trimIndent()

        exec(sqlStatement) { resultSet ->
            while (resultSet.next()) {
                result += AirQualityRecord(
                    id = resultSet.getInt(AirQualityRecords.id.nameInDatabaseCase()),
                    latitude = resultSet.getDouble(AirQualityRecords.latitude.nameInDatabaseCase()),
                    longitude = resultSet.getDouble(AirQualityRecords.longitude.nameInDatabaseCase()),
                    timestamp = resultSet.getLong(AirQualityRecords.timestamp.nameInDatabaseCase()),
                    pm10 = (resultSet.getObject(AirQualityRecords.pm10.nameInDatabaseCase()) as BigDecimal?)?.toFloat(),
                    pm25 = resultSet.getFloat(AirQualityRecords.pm25.nameInDatabaseCase()),
                    pm100 = resultSet.getFloat(AirQualityRecords.pm100.nameInDatabaseCase()),
                    providerId = resultSet.getInt(AirQualityRecords.provider.nameInDatabaseCase()),
                    metadata = resultSet.getString(AirQualityRecords.metadata.nameInDatabaseCase()),
                    createdAt = resultSet.getLong(AirQualityRecords.createdAt.nameInDatabaseCase())
                )
            }
            result
        }

        return@newSuspendedTransaction result
    }

    suspend fun read(id: Int): AirQualityRecord? = dbQuery {
        AirQualityRecords.select { AirQualityRecords.id eq id }.map { it.toAirQualityRecord() }
    }.singleOrNull()

    suspend fun readLatestSubmittedRecordByProvider(providerId: Int): AirQualityRecord? = dbQuery {
        AirQualityRecords.select { AirQualityRecords.provider eq providerId }
            .orderBy(AirQualityRecords.createdAt to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.toAirQualityRecord()
    }

    suspend fun getTotalSubmittedRecordsByProvider(providerId: Int): Long = dbQuery {
        AirQualityRecords.select { AirQualityRecords.provider eq providerId }
            .count()
    }

    suspend fun delete(id: Int) = dbQuery {
        AirQualityRecords.deleteWhere { AirQualityRecords.id eq id }
    }

    private fun ResultRow.toAirQualityRecord() = AirQualityRecord(
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