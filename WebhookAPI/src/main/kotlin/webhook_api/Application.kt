package webhook_api

import io.ktor.server.application.*
import webhook_api.plugins.configureWebhookAPI
import webhook_api.plugins.configureKoin
import webhook_api.plugins.configureRouting
import webhook_api.plugins.configureSerialization

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureSerialization()
    configureRouting()

    WebhookCaller()
    configureWebhookAPI()
}
