package model

import kotlinx.serialization.Serializable

@Serializable
data class AirQualityRecord(
    override val id: Int,
    override val latitude: Double,
    override val longitude: Double,
    override val timestamp: Long,
    val pm10: Float? = null,
    val pm25: Float,
    val pm100: Float,
    override val providerId: Int,
    override val metadata: String,
    override val createdAt: Long
): AggregatedRecord

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