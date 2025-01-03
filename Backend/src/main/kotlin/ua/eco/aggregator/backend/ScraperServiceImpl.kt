package ua.eco.aggregator.backend

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ua.eco.aggregator.base.model.Scraper
import ua.eco.aggregator.base.model.ScraperDTO

class ScraperServiceImpl(database: Database) : ScraperService {
    private object Scrapers : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", length = 50)
        val apiKey = char("api_key", length = 32)
        val description_en = varchar("description_en", 500)
        val description_uk = varchar("description_uk", 500)
        val url = varchar("url", 200)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Scrapers)
        }
    }

    override val scrapersTableIdColumn: Column<Int> = Scrapers.id

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(scraperDTO: ScraperDTO): Int = dbQuery {
        Scrapers.insert {
            it[name] = scraperDTO.name
            it[apiKey] = scraperDTO.apiKey
            it[description_en] = scraperDTO.descriptionEnglish
            it[description_uk] = scraperDTO.descriptionUkrainian
            it[url] = scraperDTO.url
        }[Scrapers.id]
    }

    override suspend fun readAll(): List<Scraper> = dbQuery {
        Scrapers.selectAll().map { it.toScraper() }
    }

    override suspend fun read(id: Int): Scraper? = dbQuery {
        Scrapers.select { Scrapers.id eq id }
            .map { it.toScraper() }
            .singleOrNull()
    }

    override suspend fun getByApiKey(apiKey: String): Scraper? = dbQuery {
        Scrapers.select { Scrapers.apiKey eq apiKey }
            .map { it.toScraper() }
            .singleOrNull()
    }

    override suspend fun update(id: Int, scraperDTO: ScraperDTO) = dbQuery {
        Scrapers.update({ Scrapers.id eq id }) {
            it[name] = scraperDTO.name
            it[apiKey] = scraperDTO.apiKey
            it[description_en] = scraperDTO.descriptionEnglish
            it[description_uk] = scraperDTO.descriptionUkrainian
            it[url] = scraperDTO.url
        }
    }

    override suspend fun delete(id: Int) = dbQuery {
        Scrapers.deleteWhere { Scrapers.id.eq(id) }
    }

    private fun ResultRow.toScraper(): Scraper {
        return Scraper(
            id = this[Scrapers.id],
            name = this[Scrapers.name],
            apiKey = this[Scrapers.apiKey],
            descriptionEnglish = this[Scrapers.description_en],
            descriptionUkrainian = this[Scrapers.description_uk],
            url = this[Scrapers.url]
        )
    }
}