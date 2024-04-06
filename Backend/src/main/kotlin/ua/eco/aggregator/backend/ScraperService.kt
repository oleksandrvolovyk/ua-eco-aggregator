package ua.eco.aggregator.backend

import org.jetbrains.exposed.sql.Column
import ua.eco.aggregator.base.model.Scraper

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
     * @param scraperName Scraper name
     * @param scraperApiKey Scraper API key
     * @return ID of the created Scraper
     */
    suspend fun create(scraperName: String, scraperApiKey: String): Int

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
     * @param scraperName New Scraper name
     * @param scraperApiKey New Scraper API key
     * @return
     */
    suspend fun update(id: Int, scraperName: String, scraperApiKey: String): Int

    /**
     * Delete Scraper by ID
     *
     * @param id Scraper ID
     * @return
     */
    suspend fun delete(id: Int): Int
}