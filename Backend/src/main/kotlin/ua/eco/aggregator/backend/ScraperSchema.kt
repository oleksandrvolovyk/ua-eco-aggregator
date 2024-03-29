package ua.eco.aggregator.backend

import kotlinx.coroutines.Dispatchers
import ua.eco.aggregator.base.model.Scraper
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class ScraperService(database: Database) {
    object Scrapers : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", length = 50)
        val apiKey = char("api_key", length = 32)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Scrapers)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(scraperName: String, scraperApiKey: String): Int = dbQuery {
        Scrapers.insert {
            it[name] = scraperName
            it[apiKey] = scraperApiKey
        }[Scrapers.id]
    }

    suspend fun readAll(): List<Scraper> = dbQuery {
        Scrapers.selectAll()
            .map { Scraper(it[Scrapers.id], it[Scrapers.name], it[Scrapers.apiKey]) }
    }

    suspend fun read(id: Int): Scraper? = dbQuery {
        Scrapers.select { Scrapers.id eq id }
            .map { Scraper(it[Scrapers.id], it[Scrapers.name], it[Scrapers.apiKey]) }
            .singleOrNull()
    }

    suspend fun getByApiKey(apiKey: String): Scraper? = dbQuery {
        Scrapers.select { Scrapers.apiKey eq apiKey }
            .map { Scraper(it[Scrapers.id], it[Scrapers.name], it[Scrapers.apiKey]) }
            .singleOrNull()
    }

    suspend fun update(id: Int, scraperName: String, scraperApiKey: String) = dbQuery {
        Scrapers.update({ Scrapers.id eq id }) {
            it[name] = scraperName
            it[apiKey] = scraperApiKey
        }
    }

    suspend fun delete(id: Int) = dbQuery {
        Scrapers.deleteWhere { Scrapers.id.eq(id) }
    }
}