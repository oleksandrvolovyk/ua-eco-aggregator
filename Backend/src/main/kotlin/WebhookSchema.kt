import kotlinx.coroutines.Dispatchers
import model.AggregatedRecord
import model.PendingWebhookCall
import model.WebhookDTO
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.java.KoinJavaComponent.inject

class WebhookService(database: Database) {
    private val airQualityService by inject<AirQualityService>(AirQualityService::class.java)
    private val radiationService by inject<RadiationService>(RadiationService::class.java)

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

        val airQualityRecordId = reference("air_quality_record_id", AirQualityService.AirQualityRecords.id).nullable()
        val radiationRecordId = reference("radiation_record_id",  RadiationService.RadiationRecords.id).nullable()
        val callbackUrl = varchar("callback_url", 300)

        override val primaryKey = PrimaryKey(id)
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
        return if (this[PendingWebhookCalls.airQualityRecordId] != null) {
            val airQualityRecord = this[PendingWebhookCalls.airQualityRecordId]?.let { airQualityService.read(it) }
            if (airQualityRecord != null) {
                PendingWebhookCall(
                    id = this[PendingWebhookCalls.id],
                    callbackUrl = this[PendingWebhookCalls.callbackUrl],
                    data = airQualityRecord
                )
            } else {
                null
            }
        } else if (this[PendingWebhookCalls.radiationRecordId] != null) {
            val radiationRecord = this[PendingWebhookCalls.radiationRecordId]?.let { radiationService.read(it) }
            if (radiationRecord != null) {
                PendingWebhookCall(
                    id = this[PendingWebhookCalls.id],
                    callbackUrl = this[PendingWebhookCalls.callbackUrl],
                    data = radiationRecord
                )
            } else {
                null
            }
        } else {
            null
        }
    }
}
