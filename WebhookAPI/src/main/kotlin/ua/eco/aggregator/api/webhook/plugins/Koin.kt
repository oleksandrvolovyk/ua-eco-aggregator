package ua.eco.aggregator.api.webhook.plugins

import ua.eco.aggregator.backend.BackendKoinModule
import io.ktor.server.application.*
import org.koin.core.context.GlobalContext
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(BackendKoinModule)
        GlobalContext.startKoin(this)
    }
}