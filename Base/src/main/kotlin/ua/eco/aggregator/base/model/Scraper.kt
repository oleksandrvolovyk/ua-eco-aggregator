package ua.eco.aggregator.base.model

import kotlinx.serialization.Serializable

@Serializable
data class Scraper(val id: Int, val name: String, val apiKey: String)

@Serializable
data class ScraperDTO(val name: String, val apiKey: String)