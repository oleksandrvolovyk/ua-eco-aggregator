package ua.eco.aggregator.backend

import org.jetbrains.exposed.sql.Column
import ua.eco.aggregator.base.model.AggregatedRecord
import ua.eco.aggregator.base.model.AggregatedRecordDTO
import ua.eco.aggregator.base.model.PaginatedData
import kotlin.reflect.KClass
import kotlin.reflect.KType

object SortFieldName {
    const val TIMESTAMP = "timestamp"
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

/**
 * Record service
 *
 * @param T Record type
 * @param TDTO Record DTO type
 */
interface RecordService<T : AggregatedRecord, TDTO : AggregatedRecordDTO> {

    /**
     * Entity data property
     *
     * @property name Name of the property
     * @property type KType of the property
     */
    class EntityDataProperty(
        val name: String,
        val type: KType
    )

    val entityClass: KClass<T>
    val entityDTOClass: KClass<TDTO>
    val entityDataProperties: List<EntityDataProperty>
    val entityClassSimpleName: String
    val recordsTableIdColumn: Column<Int>
    val recordsTableName: String

    /**
     * Create new record
     *
     * @param entityDTO Entity DTOs
     * @param scraperId The ID of the scraper that collected these records. If not specified, the scraper
     * ID will be calculated for each record separately using the apiKey property of the entity DTO
     * @return True, if entity was created
     */
    suspend fun create(entityDTO: TDTO, scraperId: Int? = null): Boolean

    /**
     * Create a list of records
     *
     * @param entityDTOs List of entity DTOs
     * @return Number of entities created
     */
    suspend fun createMany(entityDTOs: List<TDTO>): Int

    /**
     * Read a record by ID
     *
     * @param id ID of record
     * @return Record(nullable)
     */
    suspend fun read(id: Int): T?

    /**
     * Get number of total submitted records by provider
     *
     * @param providerId Provider ID
     * @return Number of total submitted records by provider
     */
    suspend fun getTotalSubmittedRecordsByProvider(providerId: Int): Long

    /**
     * Delete record by ID
     *
     * @param id Record ID
     * @return
     */
    suspend fun delete(id: Int): Int

    /**
     * Read paginated
     *
     * @param providerId If specified, filters records by provider ID
     * @param timestampStart If specified with timestampEnd, filters records by timestamp(timestampStart <= timestamp <= timestampEnd)
     * @param timestampEnd If specified with timestampStart, filters records by timestamp(timestampStart <= timestamp <= timestampEnd)
     * @param latitude If specified with longitude, filters records by coordinates
     * @param longitude If specified with latitude, filters records by coordinates
     * @param sortFieldName The name of the record field by which the sort will be performed
     * @param sortDirection The sort direction
     * @param page Page
     * @return Paginated data
     */
    suspend fun readPaginated(
        providerId: Int?,
        timestampStart: Long?,
        timestampEnd: Long?,
        latitude: Double?,
        longitude: Double?,
        sortFieldName: String,
        sortDirection: SortDirection,
        page: Long
    ): PaginatedData<T>

    /**
     * Read latest submitted records with distinct locations
     *
     * @param at If specified, ignores records with "timestamp" > "at"
     * @param maxAge If specified without "at" parameter, ignores records older than "maxAge" seconds relative to current time.
     * If specified with "at" parameter, ignores records older than "maxAge" seconds relative to "at".
     */
    suspend fun readLatestSubmittedRecordsWithDistinctLocations(
        at: Long? = null,
        maxAge: Long? = null
    ): List<T>
}