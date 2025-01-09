plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    java
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20-Beta1"
}

group = "com.koruja.kotlin.cache"
version = "0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    jvmToolchain(17)
}

subprojects {
    if (file("src/main/kotlin").isDirectory || file("src/main/resource").isDirectory) {

        apply {
            plugin("org.jetbrains.kotlin.jvm")
            plugin("org.jetbrains.kotlin.plugin.serialization")
        }

        tasks.withType<Test> { useJUnitPlatform() }

        repositories {
            mavenCentral()
        }

        dependencies {
            implementation(rootProject.libs.coroutines)
            implementation(rootProject.libs.datetime)
            implementation(rootProject.libs.serialization.core)
            implementation(rootProject.libs.serialization.json)

            testImplementation(rootProject.libs.kotest)
            testImplementation(rootProject.libs.kotest.junit5)
        }

        if (project.name == "sample") {
            tasks {
                all { enabled = false }
            }
        }
    }
}

tasks.register("buildLib") {
    dependsOn(":clean", ":build") // todo - fix this task
    doFirst {
        project(":sample").tasks.all { this.enabled = false }
    }
}
