package model

import kotlinx.serialization.Serializable

@Serializable
data class RadiationRecord(
    override val id: Int,
    override val latitude: Double,
    override val longitude: Double,
    override val timestamp: Long,
    val doseInNanoSievert: Int,
    override val providerId: Int,
    override val metadata: String,
    override val createdAt: Long
): AggregatedRecord

@Serializable
data class RadiationRecordDTO(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val doseInNanoSievert: Int,
    val apiKey: String,
    val metadata: String
)