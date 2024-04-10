package ua.eco.aggregator.backend

import kotlinx.coroutines.runBlocking
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import org.ehcache.impl.config.persistence.CacheManagerPersistenceConfiguration
import ua.eco.aggregator.base.model.Scraper
import ua.eco.aggregator.base.model.ScraperDTO
import ua.eco.aggregator.base.model.toScraper
import java.io.File

class ScraperServiceCachedImpl(
    private val delegate: ScraperService,
    storagePath: File
) : ScraperService {
    override val scrapersTableIdColumn = delegate.scrapersTableIdColumn

    private val cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .with(CacheManagerPersistenceConfiguration(storagePath))
        .withCache(
            "scraperCache",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Int::class.javaObjectType,
                Scraper::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(1000, EntryUnit.ENTRIES)
                    .offheap(10, MemoryUnit.MB)
                    .disk(100, MemoryUnit.MB, false)
            )
        )
        .build(true)

    private val scraperCache = cacheManager
        .getCache("scraperCache", Int::class.javaObjectType, Scraper::class.java)
        .apply {
            runBlocking {
                putAll(delegate.readAll().associateBy { it.id })
            }
        }

    override suspend fun create(scraperDTO: ScraperDTO): Int {
        delegate.create(scraperDTO)
            .also { scraperId ->
                scraperCache.put(scraperId, scraperDTO.toScraper(scraperId))
                return scraperId
            }
    }

    override suspend fun readAll(): List<Scraper> = scraperCache.asIterable().map { it.value }

    override suspend fun read(id: Int): Scraper? =
        scraperCache[id]
            ?: delegate.read(id)
                .also { scraper -> scraperCache.put(id, scraper) }

    override suspend fun getByApiKey(apiKey: String): Scraper? =
        scraperCache.asIterable().map { it.value }.firstOrNull { it.apiKey == apiKey }
            ?: delegate.getByApiKey(apiKey)
                ?.also { scraper -> scraperCache.put(scraper.id, scraper) }

    override suspend fun update(id: Int, scraperDTO: ScraperDTO): Int {
        scraperCache.put(id, scraperDTO.toScraper(id))
        return delegate.update(id, scraperDTO)
    }

    override suspend fun delete(id: Int): Int {
        scraperCache.remove(id)
        return delegate.delete(id)
    }
}