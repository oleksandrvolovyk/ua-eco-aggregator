import kotlinx.coroutines.Dispatchers
import model.AggregatedRecord
import model.AggregatedRecordClasses
import model.PendingWebhookCall
import model.WebhookDTO
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table.Dual.nullable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.java.KoinJavaComponent.inject

class WebhookService(database: Database) {

    private val recordServices = mutableListOf<RecordService<*, *>>()

    init {
        for (record in AggregatedRecordClasses) {
            recordServices.add(inject<RecordService<*, *>>(RecordService::class.java).value)
        }
    }

    object Webhooks : Table() {
        val id = integer("id").autoIncrement()
        val latitude = double("latitude")
        val longitude = double("longitude")
        val callbackUrl = varchar("callback_url", 300)

        override val primaryKey = PrimaryKey(id)

        init {
            uniqueIndex(latitude, longitude, callbackUrl)
        }
    }

    object PendingWebhookCalls : Table() {
        val id = long("id").autoIncrement()

        val callbackUrl = varchar("callback_url", 300)

        override val primaryKey = PrimaryKey(id)
    }

    private val referenceColumnsToRecordServicesMap = buildMap {
        for (recordService in recordServices) {
            val referenceColumn = PendingWebhookCalls
                .reference("${recordService.recordsTableName}_id", recordService.recordsTableIdColumn)
                .nullable()

            this[referenceColumn] = recordService
        }
    }

    init {
        transaction(database) {
            SchemaUtils.create(Webhooks)
            SchemaUtils.create(PendingWebhookCalls)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(webhookDTO: WebhookDTO): Boolean = dbQuery {
        return@dbQuery try {
            Webhooks.insert {
                it[latitude] = webhookDTO.latitude
                it[longitude] = webhookDTO.longitude
                it[callbackUrl] = webhookDTO.callbackUrl
            }
            true
        } catch (e: ExposedSQLException) {
            false
        }
    }

    suspend fun delete(webhookDTO: WebhookDTO): Boolean = dbQuery {
        return@dbQuery Webhooks.deleteWhere {
            (callbackUrl eq webhookDTO.callbackUrl) and
                    (latitude eq webhookDTO.latitude) and
                    (longitude eq webhookDTO.longitude)
        } != 0
    }

    suspend fun getPendingWebhookCalls(): List<PendingWebhookCall<AggregatedRecord>> = dbQuery {
        PendingWebhookCalls.selectAll().mapNotNull { it.toPendingWebhookCall() }
    }

    suspend fun removePendingWebhookCall(callId: Long) = dbQuery {
        PendingWebhookCalls.deleteWhere { id eq callId }
    }

    suspend fun deleteWebhooksByCallbackUrl(invalidCallbackUrl: String) = dbQuery {
        // Delete all webhooks with this callbackUrl
        Webhooks.deleteWhere { callbackUrl eq invalidCallbackUrl }
    }

    private suspend fun ResultRow.toPendingWebhookCall(): PendingWebhookCall<AggregatedRecord>? {
        for ((referenceColumn, recordService) in referenceColumnsToRecordServicesMap) {
            if (this[referenceColumn] != null) {
                val record = this[referenceColumn]?.let { recordService.read(it) }
                if (record != null) {
                    return PendingWebhookCall(
                        id = this[PendingWebhookCalls.id],
                        callbackUrl = this[PendingWebhookCalls.callbackUrl],
                        data = record
                    )
                }
            }
        }
        return null
    }
}
