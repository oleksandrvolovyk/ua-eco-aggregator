package ua.eco.aggregator.backend

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ua.eco.aggregator.base.model.AggregatedRecord
import ua.eco.aggregator.base.model.AggregatedRecordClasses
import ua.eco.aggregator.base.model.AggregatedRecordDTO
import java.io.File
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

        val hikariDataSource = HikariDataSource(HikariConfig().apply {
            driverClassName = "oracle.jdbc.OracleDriver"
            jdbcUrl = "jdbc:oracle:thin:@$connectionString"
            this.username = username
            this.password = password
            maximumPoolSize = 4
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            validate()
        })

        Database.connect(hikariDataSource)
    }

    val pageSize = try {
        System.getenv("PAGE_SIZE").toIntOrNull() ?: 50
    } catch (_: NullPointerException) {
        50
    }

    single<ScraperService> {
        ScraperServiceCachedImpl(
            delegate = ScraperServiceImpl(database = get()),
            storagePath = File("build/ehcache")
        )
    }

    for (record in AggregatedRecordClasses) {
        single<RecordService<out AggregatedRecord, out AggregatedRecordDTO>>(named(record.recordClass.simpleName!!)) {
            RecordServiceCachedImpl(
                delegate = RecordServiceImpl(
                    entityClass = record.recordClass,
                    entityDTOClass = record.recordDTOClass,
                    database = get(),
                    pageSize = pageSize
                ),
                storagePath = File("build/ehcache${record.recordClass.simpleName}")
            )
        }
    }
    single { WebhookService(database = get()) }
}