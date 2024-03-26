package public_api

import public_api.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureSerialization()
    configureRouting()
    configureSwaggerUI()
    configureCORS()

    configureRecordPublicAPI()
    configureScraperPublicAPI()
}
