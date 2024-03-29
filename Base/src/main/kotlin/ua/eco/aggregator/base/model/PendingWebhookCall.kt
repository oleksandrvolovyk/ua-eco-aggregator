package ua.eco.aggregator.base.model

data class PendingWebhookCall<T: AggregatedRecord>(
    val id: Long,
    val callbackUrl: String,
    val data: T
)