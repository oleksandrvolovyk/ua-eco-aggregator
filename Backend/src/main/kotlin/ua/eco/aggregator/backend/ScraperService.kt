package ua.eco.aggregator.backend

import org.jetbrains.exposed.sql.Column
import ua.eco.aggregator.base.model.Scraper
import ua.eco.aggregator.base.model.ScraperDTO

/**
 * Scraper service
 */
interface ScraperService {
    /**
     * Scrapers table ID column
     */
    val scrapersTableIdColumn: Column<Int>

    /**
     * Create new Scraper
     *
     * @param scraperDTO Scraper DTO
     * @return ID of the created Scraper
     */
    suspend fun create(scraperDTO: ScraperDTO): Int

    /**
     * Read all Scrapers
     *
     * @return A list of all Scrapers
     */
    suspend fun readAll(): List<Scraper>

    /**
     * Read a Scraper by ID
     *
     * @param id Scraper ID
     * @return Scraper(nullable)
     */
    suspend fun read(id: Int): Scraper?

    /**
     * Get Scraper by API key
     *
     * @param apiKey Scraper API key
     * @return Scraper(nullable)
     */
    suspend fun getByApiKey(apiKey: String): Scraper?

    /**
     * Update Scraper by ID
     *
     * @param id Scraper ID
     * @param scraperDTO Scraper DTO
     * @return
     */
    suspend fun update(id: Int, scraperDTO: ScraperDTO): Int

    /**
     * Delete Scraper by ID
     *
     * @param id Scraper ID
     * @return
     */
    suspend fun delete(id: Int): Int
}