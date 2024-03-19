package model

import kotlinx.serialization.Serializable

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