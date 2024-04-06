package ua.eco.aggregator.backend

import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import org.ehcache.impl.config.persistence.CacheManagerPersistenceConfiguration
import org.jetbrains.exposed.sql.Column
import ua.eco.aggregator.base.model.AggregatedRecord
import ua.eco.aggregator.base.model.AggregatedRecordDTO
import ua.eco.aggregator.base.model.PaginatedData
import java.io.File
import java.io.Serializable
import java.time.Duration
import kotlin.reflect.KClass

class RecordServiceCachedImpl<T : AggregatedRecord, TDTO : AggregatedRecordDTO>(
    private val delegate: RecordService<T, TDTO>,
    storagePath: File
) : RecordService<T, TDTO> {

    private data class PaginatedReadParameters(
        val providerId: Int?,
        val timestampStart: Long?,
        val timestampEnd: Long?,
        val latitude: Double?,
        val longitude: Double?,
        val sortFieldName: String,
        val sortDirection: SortDirection,
        val page: Long
    ) : Serializable

    private data class LatestSubmittedRecordsWithDistinctLocationsReadParameters(
        val at: Long?, val maxAge: Long?
    ) : Serializable

    override val entityClass: KClass<T> = delegate.entityClass
    override val entityDTOClass: KClass<TDTO> = delegate.entityDTOClass
    override val entityDataProperties: List<RecordService.EntityDataProperty> = delegate.entityDataProperties
    override val entityClassSimpleName: String = delegate.entityClassSimpleName
    override val recordsTableIdColumn: Column<Int> = delegate.recordsTableIdColumn
    override val recordsTableName: String = delegate.recordsTableName

    private val cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .with(CacheManagerPersistenceConfiguration(storagePath))
        .withCache(
            "${entityClassSimpleName}byId",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Int::class.javaObjectType,
                entityClass.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(1000, EntryUnit.ENTRIES)
                    .offheap(10, MemoryUnit.MB)
                    .disk(100, MemoryUnit.MB, true)
            )
        )
        .withCache(
            "paginated${entityClassSimpleName}",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                PaginatedReadParameters::class.java,
                PaginatedData::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(1000, EntryUnit.ENTRIES)
                    .offheap(10, MemoryUnit.MB)
                    .disk(100, MemoryUnit.MB, false)
            ).withExpiry(
                ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(5))
            )
        )
        .withCache(
            "latestSubmitted${entityClassSimpleName}WithDistinctLocations",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                LatestSubmittedRecordsWithDistinctLocationsReadParameters::class.java,
                List::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(1000, EntryUnit.ENTRIES)
            ).withExpiry(
                ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(5))
            )
        )
        .withCache(
            "totalSubmitted${entityClassSimpleName}byProviderId",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Int::class.javaObjectType,
                Long::class.javaObjectType,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(1000, EntryUnit.ENTRIES)
                    .offheap(10, MemoryUnit.MB)
                    .disk(100, MemoryUnit.MB, false)
            ).withExpiry(
                ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(24))
            )
        )
        .build(true)

    private val recordsByIdCache =
        cacheManager.getCache("${entityClassSimpleName}byId", Int::class.javaObjectType, entityClass.java)

    private val paginatedReadsCache = cacheManager.getCache(
        "paginated${entityClassSimpleName}",
        PaginatedReadParameters::class.java,
        PaginatedData::class.java
    )

    private val latestSubmittedRecordsWithDistinctLocationsCache = cacheManager.getCache(
        "latestSubmitted${entityClassSimpleName}WithDistinctLocations",
        LatestSubmittedRecordsWithDistinctLocationsReadParameters::class.java,
        List::class.java
    )

    private val totalSubmittedRecordsByProviderCache = cacheManager.getCache(
        "totalSubmitted${entityClassSimpleName}byProviderId",
        Int::class.javaObjectType,
        Long::class.javaObjectType
    )

    override suspend fun read(id: Int): T? =
        recordsByIdCache[id]
            ?: delegate.read(id)
                .also { record -> recordsByIdCache.put(id, record) }

    override suspend fun getTotalSubmittedRecordsByProvider(providerId: Int): Long =
        totalSubmittedRecordsByProviderCache[providerId]
            ?: delegate.getTotalSubmittedRecordsByProvider(providerId).also {
                totalSubmittedRecordsByProviderCache.put(providerId, it)
            }

    override suspend fun delete(id: Int): Int {
        recordsByIdCache.remove(id)
        return delegate.delete(id)
    }

    override suspend fun readPaginated(
        providerId: Int?,
        timestampStart: Long?,
        timestampEnd: Long?,
        latitude: Double?,
        longitude: Double?,
        sortFieldName: String,
        sortDirection: SortDirection,
        page: Long
    ): PaginatedData<T> {
        val parameters = PaginatedReadParameters(
            providerId = providerId,
            timestampStart = timestampStart,
            timestampEnd = timestampEnd,
            latitude = latitude,
            longitude = longitude,
            sortFieldName = sortFieldName,
            sortDirection = sortDirection,
            page = page,
        )

        return paginatedReadsCache[parameters] as PaginatedData<T>?
            ?: delegate.readPaginated(
                providerId,
                timestampStart,
                timestampEnd,
                latitude,
                longitude,
                sortFieldName,
                sortDirection,
                page
            ).also {
                paginatedReadsCache.put(parameters, it)
            }
    }

    override suspend fun readLatestSubmittedRecordsWithDistinctLocations(at: Long?, maxAge: Long?): List<T> {
        val parameters = LatestSubmittedRecordsWithDistinctLocationsReadParameters(
            at = at,
            maxAge = maxAge
        )

        return latestSubmittedRecordsWithDistinctLocationsCache[parameters] as List<T>?
            ?: delegate.readLatestSubmittedRecordsWithDistinctLocations(
                at, maxAge
            ).also {
                latestSubmittedRecordsWithDistinctLocationsCache.put(parameters, it)
            }
    }

    override suspend fun createMany(entityDTOs: List<TDTO>): Int = delegate.createMany(entityDTOs)

    override suspend fun create(entityDTO: TDTO, scraperId: Int?): Boolean = delegate.create(entityDTO, scraperId)
}