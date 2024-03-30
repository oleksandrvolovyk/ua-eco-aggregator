package ua.eco.aggregator.base.model

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
sealed interface AggregatedRecord {
    val id: Int
    val latitude: Double
    val longitude: Double
    val timestamp: Long
    val providerId: Int
    val metadata: String
    val createdAt: Long
}

@Serializable
sealed interface AggregatedRecordDTO {
    val latitude: Double
    val longitude: Double
    val timestamp: Long
    val apiKey: String
    val metadata: String
}

class AggregatedRecordData<T : AggregatedRecord, TDTO : AggregatedRecordDTO>(
    val recordClass: KClass<T>,
    val recordDTOClass: KClass<TDTO>,
    val apiRoute: String
)

val AggregatedRecordClasses = listOf(
    AggregatedRecordData(AirQualityRecord::class, AirQualityRecordDTO::class, "air-quality-records"),
    AggregatedRecordData(RadiationRecord::class, RadiationRecordDTO::class, "radiation-records"),
    AggregatedRecordData(TestRecord::class, TestRecordDTO::class, "test-records"),
    AggregatedRecordData(FireRecord::class, FireRecordDTO::class, "fire-records")
)