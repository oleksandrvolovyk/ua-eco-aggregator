plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "UaEcoAggregator"
include("ScraperAPI")
include("MeteoGovUaScraper")
include("SensorCommunityScraper")
include("EcoCityScraper")
include("LunMistoScraper")
include("Backend")
include("Base")
include("AdminAPI")
include("PublicAPI")
include("SaveDniproScraper")
include("EcoZagrozaGovUaScraper")
include("WebhookAPI")
