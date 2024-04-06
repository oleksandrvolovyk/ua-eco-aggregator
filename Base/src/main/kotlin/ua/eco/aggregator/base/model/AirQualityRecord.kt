package ua.eco.aggregator.base.model

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
) : AggregatedRecord, java.io.Serializable

@Serializable
data class AirQualityRecordDTO(
    override val latitude: Double = -1.0,
    override val longitude: Double = -1.0,
    override val timestamp: Long = -1,
    override val apiKey: String = "",
    override val metadata: String = "",
    val pm10: Float? = null,
    val pm25: Float = -1F,
    val pm100: Float = -1F,
) : AggregatedRecordDTO