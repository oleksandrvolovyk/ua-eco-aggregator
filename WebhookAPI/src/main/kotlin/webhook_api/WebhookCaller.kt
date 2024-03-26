package webhook_api

import WebhookService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerializationException
import webhook_api.plugins.CALLBACK_URL_VALID_RESPONSE

const val DELAY_BETWEEN_DB_CHECKS_MILLIS = 300_000L // 5 minutes
const val DELAY_BETWEEN_CALLS_MILLIS = 1_000L // 1 second
const val DELAY_BETWEEN_FAILED_DB_CALLS = 5_000L // 5 seconds

class WebhookCaller(webhookService: WebhookService) {
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
                    val responseContainer = try {
                        val response = ktorClient.post(pendingWebhookCall.callbackUrl) {
                            contentType(ContentType.Application.Json)
                            setBody(pendingWebhookCall.data)
                        }
                        ResponseContainer.Success(response)
                    } catch (e: ClientRequestException) {
                        ResponseContainer.Error.HttpError(e.response.status.value, e.message)
                    } catch (e: ServerResponseException) {
                        ResponseContainer.Error.HttpError(e.response.status.value, e.message)
                    } catch (e: IOException) {
                        ResponseContainer.Error.NetworkError
                    } catch (e: SerializationException) {
                        ResponseContainer.Error.SerializationError
                    }

                    when (responseContainer) {
                        is ResponseContainer.Success -> {
                            if (
                                responseContainer.response.status != HttpStatusCode.OK ||
                                responseContainer.response.body<String>() == CALLBACK_URL_VALID_RESPONSE
                            ) {
                                // Remove webhook because of invalid response
                                println(
                                    "Removing webhooks with \"${pendingWebhookCall.callbackUrl}\" callback url " +
                                            "because of invalid response"
                                )
                                tryUntilSuccess {
                                    webhookService.deleteWebhooksByCallbackUrl(pendingWebhookCall.callbackUrl)
                                }
                            }
                        }

                        is ResponseContainer.Error.HttpError, is ResponseContainer.Error.SerializationError -> {
                            // Remove webhook because of invalid response
                            println(
                                "Removing webhooks with \"${pendingWebhookCall.callbackUrl}\" callback url " +
                                        "because of HTTP or response serialization error"
                            )
                            tryUntilSuccess {
                                webhookService.deleteWebhooksByCallbackUrl(pendingWebhookCall.callbackUrl)
                            }
                        }

                        ResponseContainer.Error.NetworkError -> {
                            // Ignore
                        }
                    }

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

sealed class ResponseContainer<out T, out E> {
    /**
     * Represents successful network responses (200).
     */
    data class Success<T>(val response: T) : ResponseContainer<T, Nothing>()

    sealed class Error<E> : ResponseContainer<Nothing, E>() {
        /**
         * Represents server (50x) and client (40x) errors.
         */
        data class HttpError<E>(val code: Int, val errorBody: E?) : Error<E>()

        /**
         * Represent IOExceptions and connectivity issues.
         */
        data object NetworkError : Error<Nothing>()

        /**
         * Represent SerializationExceptions.
         */
        data object SerializationError : Error<Nothing>()
    }
}