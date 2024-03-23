val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val koin_version: String by project

plugins {
    kotlin("jvm")
    id("io.ktor.plugin") version "2.3.6"
    kotlin("plugin.serialization") version "1.9.21"
}

group = "volovyk.eco_aggregator"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Base"))
    implementation(project(":Backend"))

    // Koin for Ktor
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")

    // Double receive plugin
    implementation("io.ktor:ktor-server-double-receive:$ktor_version")

    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-host-common-jvm")
    implementation("io.ktor:ktor-server-status-pages-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // CORS
    implementation("io.ktor:ktor-server-cors:$ktor_version")

    // Swagger UI
    implementation("io.ktor:ktor-server-swagger:$ktor_version")

    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(19)
}