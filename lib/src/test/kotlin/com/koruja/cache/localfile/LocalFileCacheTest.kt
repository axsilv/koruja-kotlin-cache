package com.koruja.cache.localfile

import com.koruja.cache.CacheEntry
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

class LocalFileCacheTest : BehaviorSpec({

    context("Writes local cache in files at the root") {
        given("A single cache entry") {
            `when`("Insert sync the cache") {
                then("Should be able to insert, select, select all and then expires the cache") {
                    val properties = LocalFileCacheProperties(
                        baseDir = listOf("src", "test", "resources"),
                    )
                    val cache = LocalFileCache(
                        properties = properties,
                        expirationWorker = LocalFileExpirationWorkerGeneric(),
                        writer = AsynchronousWriterGeneric(),
                        asynchronousReader = AsynchronousReaderGeneric(),
                        reader = ReaderGeneric()
                    )
                    val key = UUID.randomUUID().toString()
                    val expiresAt = Clock.System.now().plus(1000.milliseconds)

                    cache.insert(
                        entry = CacheEntry(
                            key = CacheEntry.CacheEntryKey(key),
                            expiresAt = expiresAt,
                            payload = "test=payload"
                        ),
                        expiresAt = expiresAt
                    )

                    Files.exists(
                        Paths.get("src", "test", "resources", "cache", "$key.txt")
                    ) shouldBe true

                    Files.exists(
                        Paths.get(
                            "src", "test", "resources", "expirations",
                            expiresAt.toString().replace(":", "_"), "$key.txt"
                        )
                    ) shouldBe true

                    delay(10.seconds)
                }
            }
        }
    }
})
