package model

import kotlinx.serialization.Serializable

@Serializable
data class AirQualityRecord(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val pm10: Float? = null,
    val pm25: Float,
    val pm100: Float,
    val providerId: Int,
    val metadata: String,
    val createdAt: Long
)

@Serializable
data class AirQualityRecordDTO(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val pm10: Float? = null,
    val pm25: Float,
    val pm100: Float,
    val apiKey: String,
    val metadata: String
)