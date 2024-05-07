package ua.eco.aggregator.api.webhook.plugins

import ua.eco.aggregator.backend.WebhookService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ua.eco.aggregator.base.model.WebhookDTO
import org.koin.ktor.ext.inject
import ua.eco.aggregator.api.webhook.WebhookCaller

const val CALLBACK_URL_VALID_RESPONSE = "UaEcoAggregator"

fun Application.configureWebhookAPI() {
    val webhookService by inject<WebhookService>()
    val webhookCaller = WebhookCaller(webhookService)

    val ktorClient = HttpClient(CIO)

    routing {
        route("/subscribe") {
            post {
                val webhookDTO = call.receive<WebhookDTO>()

                if (!validateCallbackUrl(webhookDTO.callbackUrl, ktorClient)) {
                    call.respond(HttpStatusCode.BadRequest, "Callback URL is invalid!")
                } else {
                    val webhookAdded = webhookService.create(webhookDTO)

                    if (webhookAdded) {
                        call.respond(HttpStatusCode.OK, "Webhook added!")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Failed to add webhook!")
                    }
                }
            }
        }

        route("/unsubscribe") {
            post {
                val webhookDTO = call.receive<WebhookDTO>()

                val webhookRemoved = webhookService.delete(webhookDTO)

                if (webhookRemoved) {
                    call.respond(HttpStatusCode.OK, "Webhook removed!")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Failed to remove webhook!")
                }
            }
        }

        route("/subscriptions") {
            get {
                val url = call.parameters["url"] ?: throw IllegalArgumentException("Provide a callback url!")

                call.respond(HttpStatusCode.OK, webhookService.getByUrl(url))
            }
        }

        route("/test") {
            get {
                val url = call.parameters["url"] ?: throw IllegalArgumentException("Provide a callback url to test!")

                if (validateCallbackUrl(url, ktorClient)) {
                    call.respond(HttpStatusCode.OK, "Callback URL is valid!")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Callback URL is invalid!")
                }
            }
        }
    }
}

private suspend fun validateCallbackUrl(url: String, client: HttpClient): Boolean {
    val response = client.get(url)

    return response.status == HttpStatusCode.OK && response.body<String>() == CALLBACK_URL_VALID_RESPONSE
}