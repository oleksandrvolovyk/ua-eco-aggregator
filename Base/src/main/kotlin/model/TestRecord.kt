package model

import kotlinx.serialization.Serializable

@Serializable
data class TestRecord(
    override val id: Int,
    override val latitude: Double,
    override val longitude: Double,
    override val timestamp: Long,
    override val providerId: Int,
    override val metadata: String,
    override val createdAt: Long,
    val testData: Int,
    val testNullableData: Int? = null,
    val testFloatData: Float
): AggregatedRecord

@Serializable
data class TestRecordDTO(
    override val latitude: Double,
    override val longitude: Double,
    override val timestamp: Long,
    override val apiKey: String,
    override val metadata: String,
    val testData: Int,
    val testNullableData: Int? = null,
    val testFloatData: Float
): AggregatedRecordDTO