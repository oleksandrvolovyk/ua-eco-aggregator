package webhook_api

import WebhookService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import org.koin.java.KoinJavaComponent.inject

const val DELAY_BETWEEN_DB_CHECKS_MILLIS = 300_000L // 5 minutes
const val DELAY_BETWEEN_CALLS_MILLIS = 1_000L // 1 second
const val DELAY_BETWEEN_FAILED_DB_CALLS = 5_000L // 5 seconds

class WebhookCaller {
    private val webhookService by inject<WebhookService>(WebhookService::class.java)

    private val coroutineScope = CoroutineScope(Job())

    private val ktorClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    init {
        coroutineScope.launch {
            while (coroutineScope.isActive) {
                println("Checking pending webhook calls.")
                val pendingWebhookCalls = tryUntilSuccess { webhookService.getPendingWebhookCalls() }
                println("Sending ${pendingWebhookCalls?.size} webhook calls ")
                pendingWebhookCalls?.forEach { pendingWebhookCall ->
                    try {
                        ktorClient.post(pendingWebhookCall.callbackUrl) {
                            contentType(ContentType.Application.Json)
                            setBody(pendingWebhookCall.data)
                        }
                    } catch (_: Throwable) {
                    }

                    // TODO: Remove webhook if response is invalid

                    tryUntilSuccess {
                        webhookService.removePendingWebhookCall(pendingWebhookCall.id)
                    }

                    delay(DELAY_BETWEEN_CALLS_MILLIS)
                }
                delay(DELAY_BETWEEN_DB_CHECKS_MILLIS)
            }
        }
    }

    private suspend inline fun <T> tryUntilSuccess(block: () -> T): T? {
        while (coroutineScope.isActive) {
            try {
                return block()
            } catch (_: Throwable) {
                delay(DELAY_BETWEEN_FAILED_DB_CALLS)
            }
        }
        return null
    }
}