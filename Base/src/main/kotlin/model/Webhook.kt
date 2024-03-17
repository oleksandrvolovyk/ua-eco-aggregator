package model

import kotlinx.serialization.Serializable

@Serializable
data class Webhook(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val callbackUrl: String
)

@Serializable
data class WebhookDTO(
    val latitude: Double,
    val longitude: Double,
    val callbackUrl: String
)