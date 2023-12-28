package plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
        GlobalContext.startKoin(this)
    }
}

val appModule = module {
    single {
        Database.connect(
            url = "jdbc:postgresql://localhost:5432/scraper",
            user = "postgres",
            driver = "org.postgresql.Driver",
            password = "password"
        )
    }
    single { ScraperService(get()) }
    single { AirQualityService(get()) }
    single { RadiationService(get()) }
}