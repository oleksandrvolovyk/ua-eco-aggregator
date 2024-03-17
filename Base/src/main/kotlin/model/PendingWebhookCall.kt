package model

data class PendingWebhookCall<T>(
    val id: Long,
    val callbackUrl: String,
    val data: T
)