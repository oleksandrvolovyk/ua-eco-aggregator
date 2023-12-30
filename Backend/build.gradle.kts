plugins {
    kotlin("jvm")
}

val koin_version: String by project

val postgresql_version: String by project
val exposed_version: String by project

group = "volovyk.eco_aggregator"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Base"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Koin
    implementation("io.insert-koin:koin-core:3.5.3")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.postgresql:postgresql:$postgresql_version")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(19)
}