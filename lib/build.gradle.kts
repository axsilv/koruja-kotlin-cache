plugins {
    alias(libs.plugins.kotlin.jvm)

    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.coroutines)
    implementation(libs.datetime)

    testImplementation(libs.kotest)
    testImplementation(libs.kotest.junit5)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType<Test> { useJUnitPlatform() }
