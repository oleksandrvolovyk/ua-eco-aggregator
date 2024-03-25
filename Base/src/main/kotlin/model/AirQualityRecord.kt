package model

import kotlinx.serialization.Serializable

@Serializable
data class AirQualityRecord(
    override val id: Int,
    override val latitude: Double,
    override val longitude: Double,
    override val timestamp: Long,
    override val providerId: Int,
    override val metadata: String,
    override val createdAt: Long,
    val pm10: Float? = null,
    val pm25: Float,
    val pm100: Float,
): AggregatedRecord

@Serializable
data class AirQualityRecordDTO(
    override val latitude: Double,
    override val longitude: Double,
    override val timestamp: Long,
    override val apiKey: String,
    override val metadata: String,
    val pm10: Float? = null,
    val pm25: Float,
    val pm100: Float,
): AggregatedRecordDTO