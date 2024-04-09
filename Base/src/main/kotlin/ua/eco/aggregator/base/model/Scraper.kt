package ua.eco.aggregator.base.model

import kotlinx.serialization.Serializable

@Serializable
data class Scraper(
    val id: Int,
    val name: String,
    val apiKey: String,
    val descriptionEnglish: String = "",
    val descriptionUkrainian: String = "",
    val url: String = ""
) : java.io.Serializable

@Serializable
data class ScraperDTO(val name: String, val apiKey: String)