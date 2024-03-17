package webhook_api.plugins

import BackendKoinModule
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