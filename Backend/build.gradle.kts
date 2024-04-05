plugins {
    kotlin("jvm")
}

val koin_version: String by project

val postgresql_version: String by project
val exposed_version: String by project

val hikaricp_version: String by project
val ehcache_version: String by project

group = "volovyk.eco_aggregator"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Base"))
    implementation(kotlin("reflect"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Koin
    implementation("io.insert-koin:koin-core:3.5.3")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("com.oracle.database.jdbc:ojdbc11:23.2.0.0")

    // HikariCP
    implementation("com.zaxxer:HikariCP:$hikaricp_version")
    // Ehcache
    implementation("org.ehcache:ehcache:$ehcache_version")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(19)
}