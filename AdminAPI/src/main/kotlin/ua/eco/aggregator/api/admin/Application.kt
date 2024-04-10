package ua.eco.aggregator.api.admin

import io.ktor.server.application.*
import ua.eco.aggregator.api.admin.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureSerialization()
    configureThymeleaf()
    configureRouting()
    configureAuth()

    configureAPI()
    configureFrontend()
}
