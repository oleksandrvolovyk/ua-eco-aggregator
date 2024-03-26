import kotlinx.coroutines.Dispatchers
import model.AggregatedRecord
import model.AggregatedRecordDTO
import model.PaginatedData
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table.Dual.nullable
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.java.KoinJavaComponent.inject
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

object SortFieldName {
    const val TIMESTAMP = "timestamp"
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

private fun SortDirection.toSortOrder(): SortOrder =
    if (this == SortDirection.ASCENDING) SortOrder.ASC else SortOrder.DESC

class RecordService<T : AggregatedRecord, TDTO : AggregatedRecordDTO>(
    private val entityClass: KClass<T>,
    val entityDTOClass: KClass<TDTO>,
    database: Database,
    private val pageSize: Int
) {
    class EntityDataProperty(
        val name: String,
        val type: KType
    )

    val entityDataProperties =
        entityClass.primaryConstructor!!.parameters.mapIndexedNotNull { index, parameter ->
            if (index >= AggregatedRecord::class.memberProperties.size) {
                EntityDataProperty(parameter.name!!, parameter.type)
            } else {
                null
            }
        }

    private val entityIntDataColumns = mutableListOf<Column<Int>>()
    private val entityNullableIntDataColumns = mutableListOf<Column<Int?>>()
    private val entityLongDataColumns = mutableListOf<Column<Long>>()
    private val entityNullableLongDataColumns = mutableListOf<Column<Long?>>()
    private val entityFloatDataColumns = mutableListOf<Column<Float>>()
    private val entityNullableFloatDataColumns = mutableListOf<Column<Float?>>()
    private val entityDoubleDataColumns = mutableListOf<Column<Double>>()
    private val entityNullableDoubleDataColumns = mutableListOf<Column<Double?>>()
    private val entityBooleanDataColumns = mutableListOf<Column<Boolean>>()
    private val entityNullableBooleanDataColumns = mutableListOf<Column<Boolean?>>()

    private val entityDataColumns = listOf(
        entityIntDataColumns,
        entityNullableIntDataColumns,
        entityLongDataColumns,
        entityNullableLongDataColumns,
        entityFloatDataColumns,
        entityNullableFloatDataColumns,
        entityDoubleDataColumns,
        entityNullableDoubleDataColumns,
        entityBooleanDataColumns,
        entityNullableBooleanDataColumns
    )

    private val scraperService by inject<ScraperService>(ScraperService::class.java)

    private val RecordsTable = object : Table(name = entityClass.simpleName!!) {
        val id = integer("id").autoIncrement()
        val latitude = double("latitude")
        val longitude = double("longitude")
        val timestamp = long("timestamp")
        val provider = reference("provider", ScraperService.Scrapers.id)
        val metadata = varchar("metadata", length = 500)
        val createdAt = long("createdAt")

        override val primaryKey = PrimaryKey(id)

        init {
            uniqueIndex(latitude, longitude, timestamp, provider)
        }
    }

    val entityClassSimpleName = entityClass.simpleName
    val recordsTableName = RecordsTable.nameInDatabaseCase()
    val recordsTableIdColumn = RecordsTable.id

    private fun Table.allFieldsAsString(tableName: String = this.tableName) =
        fields.joinToString(", ") { "$tableName.${(it as Column<*>).name}" }

    init {
        transaction(database) {
            for (property in entityDataProperties) {
                when (property.type) {
                    typeOf<Int>() -> entityIntDataColumns.add(RecordsTable.integer(property.name))
                    typeOf<Int?>() -> entityNullableIntDataColumns.add(RecordsTable.integer(property.name).nullable())
                    typeOf<Long>() -> entityLongDataColumns.add(RecordsTable.long(property.name))
                    typeOf<Long?>() -> entityNullableLongDataColumns.add(RecordsTable.long(property.name).nullable())
                    typeOf<Float>() -> entityFloatDataColumns.add(RecordsTable.float(property.name))
                    typeOf<Float?>() -> entityNullableFloatDataColumns.add(RecordsTable.float(property.name).nullable())
                    typeOf<Double>() -> entityDoubleDataColumns.add(RecordsTable.double(property.name))
                    typeOf<Double?>() -> entityNullableDoubleDataColumns.add(
                        RecordsTable.double(property.name).nullable()
                    )

                    typeOf<Boolean>() -> entityBooleanDataColumns.add(RecordsTable.bool(property.name))
                    typeOf<Boolean?>() -> entityNullableBooleanDataColumns.add(
                        RecordsTable.bool(property.name).nullable()
                    )

                    else -> throw IllegalArgumentException(
                        "${entityClass.simpleName} member property ${property.name}" +
                                " type ${property.type} is not allowed."
                    )
                }
            }
            SchemaUtils.create(RecordsTable)
        }
    }

    private val fieldsIndex = RecordsTable.realFields.toSet().mapIndexed { index, expression -> expression to index }
        .toMap()

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private suspend fun create(entityDTO: TDTO): Boolean {
        val scraper = scraperService.getByApiKey(entityDTO.apiKey)!!

        return try {
            RecordsTable.insert { table ->
                table[latitude] = entityDTO.latitude
                table[longitude] = entityDTO.longitude
                table[timestamp] = entityDTO.timestamp
                table[provider] = scraper.id
                table[metadata] = entityDTO.metadata
                table[createdAt] = Instant.now().epochSecond

                table.setColumnValues(entityIntDataColumns, entityDTO)
                table.setColumnValues(entityNullableIntDataColumns, entityDTO)
                table.setColumnValues(entityLongDataColumns, entityDTO)
                table.setColumnValues(entityNullableLongDataColumns, entityDTO)
                table.setColumnValues(entityFloatDataColumns, entityDTO)
                table.setColumnValues(entityNullableFloatDataColumns, entityDTO)
                table.setColumnValues(entityDoubleDataColumns, entityDTO)
                table.setColumnValues(entityNullableDoubleDataColumns, entityDTO)
                table.setColumnValues(entityBooleanDataColumns, entityDTO)
                table.setColumnValues(entityNullableBooleanDataColumns, entityDTO)
            }.insertedCount != 0
        } catch (e: ExposedSQLException) {
            false
        }
    }

    suspend fun createMany(entityDTOs: List<TDTO>): Int = dbQuery {
        var addedCounter = 0

        entityDTOs.forEach { entityDTO ->
            if (create(entityDTO)) {
                addedCounter++
            }
        }

        return@dbQuery addedCounter
    }

    private inline fun <reified T> InsertStatement<Number>.setColumnValues(columns: List<Column<T>>, entityDTO: TDTO) {
        for (column in columns) {
            this[column] = getInstanceProperty<T>(entityDTO, column.name)
        }
    }

    suspend fun read(id: Int): T? = dbQuery {
        RecordsTable.select { RecordsTable.id eq id }.map { it.toEntity() }
    }.singleOrNull()

    suspend fun readLatestSubmittedRecordByProvider(providerId: Int): T? = dbQuery {
        RecordsTable.select { RecordsTable.provider eq providerId }
            .orderBy(RecordsTable.createdAt to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.toEntity()
    }

    suspend fun getTotalSubmittedRecordsByProvider(providerId: Int): Long = dbQuery {
        RecordsTable.select { RecordsTable.provider eq providerId }.count()
    }

    suspend fun delete(id: Int) = dbQuery {
        RecordsTable.deleteWhere { RecordsTable.id eq id }
    }

    suspend fun readPaginated(
        providerId: Int?,
        timestampStart: Long?,
        timestampEnd: Long?,
        latitude: Double?,
        longitude: Double?,
        sortFieldName: String,
        sortDirection: SortDirection,
        page: Long
    ): PaginatedData<T> = dbQuery {
        var query = RecordsTable.selectAll()
        // 1. Apply filters
        // 1.1 Filter by providerId
        if (providerId != null) {
            query = query.andWhere { RecordsTable.provider eq providerId }
        }

        // 1.2 Filter by time period
        if (timestampStart != null && timestampEnd != null) {
            query =
                query.andWhere { (RecordsTable.timestamp greaterEq timestampStart) and (RecordsTable.timestamp lessEq timestampEnd) }
        }

        // 1.3 Filter by location
        if (latitude != null && longitude != null) {
            query =
                query.andWhere { (RecordsTable.latitude eq latitude) and (RecordsTable.longitude eq longitude) }
        }

        // 2. Apply sorting
        query = when (sortFieldName) {
            SortFieldName.TIMESTAMP -> query.orderBy(RecordsTable.timestamp to sortDirection.toSortOrder())

            else -> {
                val sortFieldColumn = entityDataColumns.flatten().firstOrNull { it.name == sortFieldName }
                if (sortFieldColumn == null) {
                    throw IllegalArgumentException("Sort field $sortFieldName not found in entity ${entityClass.simpleName}")
                }
                query.orderBy(sortFieldColumn to sortDirection.toSortOrder())
            }
        }

        val totalRecords = query.count()

        // 3. Apply paging
        val data = query
            .limit(pageSize, page * pageSize)
            .map { it.toEntity() }

        return@dbQuery PaginatedData(
            page = page,
            maxPageNumber = totalRecords / pageSize,
            itemsPerPage = pageSize,
            totalItemsCount = totalRecords,
            data = data
        )
    }

    suspend fun readLatestSubmittedRecordsWithDistinctLocations(
        at: Long? = null,
        maxAge: Long? = null
    ): List<T> = dbQuery {
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
                SELECT ${RecordsTable.allFieldsAsString("t1")}
                FROM ${RecordsTable.nameInDatabaseCase()} t1
                JOIN (
                    SELECT latitude, longitude, MAX(timestamp) AS latest_timestamp
                    FROM ${RecordsTable.nameInDatabaseCase()}
                    $sqlWhereTimestamp
                    GROUP BY latitude, longitude
                ) t2 ON t1.latitude = t2.latitude AND t1.longitude = t2.longitude AND t1.timestamp = t2.latest_timestamp
            """.trimIndent()

        nativeSelect(sqlStatement).map { it.toEntity() }
    }

    private fun ResultRow.toEntity(): T {
        val constructorArguments = buildList<Any?> {
            add(this@toEntity[RecordsTable.id])
            add(this@toEntity[RecordsTable.latitude])
            add(this@toEntity[RecordsTable.longitude])
            add(this@toEntity[RecordsTable.timestamp])
            add(this@toEntity[RecordsTable.provider])
            add(this@toEntity[RecordsTable.metadata])
            add(this@toEntity[RecordsTable.createdAt])

            for (entityDataProperty in entityDataProperties) {
                add(
                    when (entityDataProperty.type) {
                        typeOf<Int>() -> this@toEntity[entityIntDataColumns.first { it.name == entityDataProperty.name }]
                        typeOf<Int?>() -> this@toEntity[entityNullableIntDataColumns.first { it.name == entityDataProperty.name }]
                        typeOf<Long>() -> this@toEntity[entityLongDataColumns.first { it.name == entityDataProperty.name }]
                        typeOf<Long?>() -> this@toEntity[entityNullableLongDataColumns.first { it.name == entityDataProperty.name }]
                        typeOf<Float>() -> this@toEntity[entityFloatDataColumns.first { it.name == entityDataProperty.name }]
                        typeOf<Float?>() -> this@toEntity[entityNullableFloatDataColumns.first { it.name == entityDataProperty.name }]
                        typeOf<Double>() -> this@toEntity[entityDoubleDataColumns.first { it.name == entityDataProperty.name }]
                        typeOf<Double?>() -> this@toEntity[entityNullableDoubleDataColumns.first { it.name == entityDataProperty.name }]
                        typeOf<Boolean>() -> this@toEntity[entityBooleanDataColumns.first { it.name == entityDataProperty.name }]
                        typeOf<Boolean?>() -> this@toEntity[entityNullableBooleanDataColumns.first { it.name == entityDataProperty.name }]
                        else -> throw IllegalArgumentException(
                            "${entityClass.simpleName} member property ${entityDataProperty.name}" +
                                    " type ${entityDataProperty.type} is not allowed."
                        )
                    }
                )
            }
        }
        return entityClass.primaryConstructor!!.call(*constructorArguments.toTypedArray())
    }

    private fun nativeSelect(query: String): List<ResultRow> {
        val resultRows = mutableListOf<ResultRow>()
        TransactionManager.current().exec(query) { resultSet ->
            while (resultSet.next()) {
                resultRows.add(ResultRow.create(resultSet, fieldsIndex))
            }
        }

        return resultRows
    }
}

@Suppress("UNCHECKED_CAST")
private fun <R> getInstanceProperty(instance: Any, propertyName: String): R {
    val property = instance::class.members
        // don't cast here to <Any, R>, it would succeed silently
        .first { it.name == propertyName } as KProperty1<Any, *>
    // force a invalid cast exception if incorrect type here
    return property.get(instance) as R
}