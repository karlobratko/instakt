import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    application
}

group = "hr.kbratko"
version = "0.0.1"

repositories {
    mavenCentral()
}

application {
    mainClass.set("hr.kbratko.instakt.infrastructure.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.kotlinx.serializations)

    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)

    implementation(libs.bundles.flyway)
    implementation(libs.hikari)
    implementation(libs.bundles.exposed)
    implementation(libs.kreds)
    implementation(libs.db.h2)
    implementation(libs.db.postgresql)

    implementation(libs.bundles.kache)

    implementation(libs.bundles.arrow)
    implementation(libs.bundles.suspendapp)

    implementation(libs.micrometer)
    implementation(libs.logback)

    implementation(libs.mail)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)

    implementation(libs.s3)
    implementation(libs.tika)

    testImplementation(libs.test.ktor.server)
    testImplementation(libs.test.kotlinx.coroutines)
    testImplementation(libs.bundles.test.kotest.core)
    testImplementation(libs.bundles.test.kotest.assertions)
    testImplementation(libs.bundles.test.kotest.extensions)
    testImplementation(libs.test.mockk)
}

tasks
    .withType<KotlinCompile>()
    .configureEach {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        }
    }

tasks.register<JavaExec>("runDevelopment") {
    group = "application"
    description = "Runs the application in development mode."

    dependsOn("classes")

    mainClass.set("hr.kbratko.instakt.infrastructure.ApplicationKt")
    classpath = sourceSets["main"].runtimeClasspath

    jvmArgs("-Dio.ktor.development=true")
}

tasks.register("rebuild") {
    group = "build"
    description = "Rebuilds the project for fast development."

    dependsOn("classes")
}

tasks.test {
    useJUnitPlatform()
}
