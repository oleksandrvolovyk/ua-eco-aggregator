package ua.eco.aggregator.base.model

import kotlinx.serialization.Serializable

@Serializable
data class FireRecord(
    override val id: Int,
    override val latitude: Double,
    override val longitude: Double,
    override val timestamp: Long,
    override val providerId: Int,
    override val metadata: String,
    override val createdAt: Long,
    val scan: Double,
    val track: Double,
    val confidence: Int?,
    val fireRadiativePower: Double
) : AggregatedRecord, java.io.Serializable

@Serializable
data class FireRecordDTO(
    override val latitude: Double = -1.0,
    override val longitude: Double = -1.0,
    override val timestamp: Long = -1,
    override val apiKey: String = "",
    override val metadata: String = "",
    val scan: Double = -1.0,
    val track: Double = -1.0,
    val confidence: Int? = null,
    val fireRadiativePower: Double = -1.0
) : AggregatedRecordDTO