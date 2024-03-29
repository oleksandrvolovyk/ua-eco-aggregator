package ua.eco.aggregator.api.webhook

import io.ktor.server.application.*
import ua.eco.aggregator.api.webhook.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureSerialization()
    configureRouting()
    configureSwaggerUI()
    configureCORS()

    configureWebhookAPI()
}
