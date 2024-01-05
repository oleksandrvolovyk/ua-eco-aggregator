package admin_api

import admin_api.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureSerialization()
    configureThymeleaf()
    configureRouting()

    configureAPI()
    configureFrontend()
}
