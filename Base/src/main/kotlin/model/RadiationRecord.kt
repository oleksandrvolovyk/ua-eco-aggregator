package model

import kotlinx.serialization.Serializable

@Serializable
data class RadiationRecord(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val doseInNanoSievert: Int,
    val providerId: Int,
    val metadata: String,
    val createdAt: Long
)

@Serializable
data class RadiationRecordDTO(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val doseInNanoSievert: Int,
    val apiKey: String,
    val metadata: String
)