package model

import kotlinx.serialization.Serializable

@Serializable
data class RadiationRecord(
    override val id: Int,
    override val latitude: Double,
    override val longitude: Double,
    override val timestamp: Long,
    override val providerId: Int,
    override val metadata: String,
    override val createdAt: Long,
    val doseInNanoSievert: Int,
): AggregatedRecord

@Serializable
data class RadiationRecordDTO(
    override val latitude: Double,
    override val longitude: Double,
    override val timestamp: Long,
    override val apiKey: String,
    override val metadata: String,
    val doseInNanoSievert: Int,
): AggregatedRecordDTO