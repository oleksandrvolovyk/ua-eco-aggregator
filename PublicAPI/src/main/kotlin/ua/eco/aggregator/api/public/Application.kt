package ua.eco.aggregator.api.public

import io.ktor.server.application.*
import ua.eco.aggregator.api.public.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureSerialization()
    configureRouting()
    configureSwaggerUI()
    configureCORS()
    configureCachingHeaders()

    configureRecordPublicAPI()
    configureScraperPublicAPI()
}
