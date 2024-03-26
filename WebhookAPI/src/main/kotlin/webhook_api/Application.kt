package webhook_api

import io.ktor.server.application.*
import webhook_api.plugins.*

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
