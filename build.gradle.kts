val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val dotenv_version: String by project

plugins {
    kotlin("jvm") version "2.1.21"
    id("io.ktor.plugin") version "3.1.3"
    kotlin("plugin.serialization") version "2.1.21"
}

kotlin {
    jvmToolchain(21) // ganti dari 25 ke 21
}

group = "org.course"
version = "0.0.1"

repositories {
    mavenCentral()
}

application {
    mainClass = "org.course.ApplicationKt"
}

ktor {
    fatJar {
        archiveFileName.set("course-app-be.jar")
    }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:3.1.3")
    implementation("io.ktor:ktor-server-status-pages:3.1.3")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.postgresql:postgresql:42.7.9")
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.61.0")
    implementation("io.insert-koin:koin-ktor:4.0.4")
    implementation("io.insert-koin:koin-logger-slf4j:4.0.4")
    implementation("io.github.cdimascio:dotenv-kotlin:$dotenv_version")
    implementation("org.mindrot:jbcrypt:0.4")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}