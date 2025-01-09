plugins {
    alias(libs.plugins.kotlin.jvm)

    `java-library`
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20-Beta1"

}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.coroutines)
    implementation(libs.datetime)
    implementation(libs.serialization.core)
    implementation(libs.serialization.json)

    testImplementation(libs.kotest)
    testImplementation(libs.kotest.junit5)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType<Test> { useJUnitPlatform() }