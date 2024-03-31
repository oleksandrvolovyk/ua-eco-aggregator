package ua.eco.aggregator.backend

import ua.eco.aggregator.base.model.AggregatedRecordClasses
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.system.exitProcess

val BackendKoinModule = module {
    single {
        val connectionString = System.getenv("DB_CONNECTION_STRING")
        val username = System.getenv("DB_USER")
        val password = System.getenv("DB_PASS")

        if (connectionString == null || username == null || password == null) {
            println("Required environment variable(s) not found!")
            println("DB_CONNECTION_STRING = $connectionString")
            println("DB_USER = $username")
            println("DB_PASS = $password")
            exitProcess(1)
        }

        Database.connect(
            url = "jdbc:oracle:thin:@$connectionString",
            user = username,
            driver = "oracle.jdbc.OracleDriver",
            password = password
        )
    }

    val pageSize = try {
        System.getenv("PAGE_SIZE").toIntOrNull() ?: 50
    } catch (_: NullPointerException) {
        50
    }

    single { ScraperService(database = get()) }
    for (record in AggregatedRecordClasses) {
        single(named(record.recordClass.simpleName!!)) {
            RecordService(
                entityClass = record.recordClass,
                entityDTOClass = record.recordDTOClass,
                database = get(),
                pageSize = pageSize
            )
        }
    }
    single { WebhookService(database = get()) }
}