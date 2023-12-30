package plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.system.exitProcess

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
        GlobalContext.startKoin(this)
    }
}

val appModule = module {
    single {
        val host = System.getenv("POSTGRES_HOST")
        val port = System.getenv("POSTGRES_PORT")
        val dbName = System.getenv("POSTGRES_DB_NAME")
        val username = System.getenv("POSTGRES_USER")
        val password = System.getenv("POSTGRES_PASS")

        if (host == null || port == null || dbName == null || username == null || password == null) {
            println("Required environment variable(s) not found!")
            println("POSTGRES_HOST = $host")
            println("POSTGRES_PORT = $port")
            println("POSTGRES_DB_NAME = $dbName")
            println("POSTGRES_USER = $username")
            println("POSTGRES_PASS = $password")
            exitProcess(1)
        }

        Database.connect(
            url = "jdbc:postgresql://${host}:${port}/${dbName}",
            user = username,
            driver = "org.postgresql.Driver",
            password = password
        )
    }
    single { ScraperService(get()) }
    single { AirQualityService(get()) }
    single { RadiationService(get()) }
}