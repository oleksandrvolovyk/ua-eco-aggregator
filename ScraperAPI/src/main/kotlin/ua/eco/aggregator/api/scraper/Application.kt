package ua.eco.aggregator.api.scraper

import io.ktor.server.application.*
import ua.eco.aggregator.api.scraper.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureSerialization()
    configureScraperAPI()
    configureHTTP()
    configureRouting()
}
