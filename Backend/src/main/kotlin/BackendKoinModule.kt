import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module
import kotlin.system.exitProcess

val BackendKoinModule = module {
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

    val pageSize = try {
        System.getenv("PAGE_SIZE").toIntOrNull() ?: 50
    } catch (_: NullPointerException) {
        50
    }

    single { ScraperService(database = get()) }
    single { AirQualityService(database = get(), pageSize = pageSize) }
    single { RadiationService(database = get(), pageSize = pageSize) }
}