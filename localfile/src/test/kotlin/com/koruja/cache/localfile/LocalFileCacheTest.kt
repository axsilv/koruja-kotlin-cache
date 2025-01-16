package com.koruja.cache.localfile

import com.koruja.cache.core.CacheEntry
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class LocalFileCacheTest :
    BehaviorSpec({
        context("Writes local cache in files at the root") {
            given("A single cache entry") {
                `when`("Insert sync the cache") {
                    then("Should be able to insert, select, select all and then expires the cache") {
                        Files.createDirectory(Path.of("src", "test", "resources", "sync"))
                        Files.createDirectory(Path.of("src", "test", "resources", "sync", "cache"))
                        Files.createDirectory(Path.of("src", "test", "resources", "sync", "expirations"))

                        val properties =
                            LocalFileCacheProperties(
                                baseDir = listOf("src", "test", "resources", "sync"),
                            )
                        val cache =
                            LocalFileCache(
                                properties = properties,
                                expirationWorker = LocalFileExpirationWorkerGeneric(),
                                writer = AsynchronousWriterGeneric(),
                                asynchronousReader = AsynchronousReaderGeneric(),
                                reader = ReaderGeneric(),
                            )
                        val key = UUID.randomUUID().toString()
                        val expiresAt = Clock.System.now().plus(5.seconds)

                        cache.insert(
                            entry =
                                CacheEntry(
                                    key = CacheEntry.CacheEntryKey(key),
                                    expiresAt = expiresAt,
                                    payload = "test=payload",
                                ),
                            expiresAt = expiresAt,
                        )

                        cache.select(CacheEntry.CacheEntryKey(key)).getOrNull().shouldNotBeNull()
                        cache.selectAsync(CacheEntry.CacheEntryKey(key)).getOrNull().shouldNotBeNull()
                        cache
                            .selectAll()
                            .getOrNull()
                            ?.first()
                            .shouldNotBeNull()
                        cache
                            .selectAllAsync()
                            .getOrNull()
                            ?.first()
                            .shouldNotBeNull()

                        delay(15.seconds)

                        Files.deleteIfExists(Path.of("src", "test", "resources", "sync", "cache"))
                        Files.deleteIfExists(Path.of("src", "test", "resources", "sync", "expirations"))
                        Files.deleteIfExists(Path.of("src", "test", "resources", "sync"))
                    }
                }

                `when`("Insert async the cache") {
                    then("Should be able to insert, select, select all and then expires the cache") {
                        Files.createDirectory(Path.of("src", "test", "resources", "async"))
                        Files.createDirectory(Path.of("src", "test", "resources", "async", "cache"))
                        Files.createDirectory(Path.of("src", "test", "resources", "async", "expirations"))

                        val properties =
                            LocalFileCacheProperties(
                                baseDir = listOf("src", "test", "resources", "async"),
                            )
                        val cache =
                            LocalFileCache(
                                properties = properties,
                                expirationWorker = LocalFileExpirationWorkerGeneric(),
                                writer = AsynchronousWriterGeneric(),
                                asynchronousReader = AsynchronousReaderGeneric(),
                                reader = ReaderGeneric(),
                            )
                        val key = UUID.randomUUID().toString()
                        val expiresAt = Clock.System.now().plus(5.seconds)

                        cache.insertAsync(
                            entry =
                                CacheEntry(
                                    key = CacheEntry.CacheEntryKey(key),
                                    expiresAt = expiresAt,
                                    payload = "test=payload",
                                ),
                            expiresAt = expiresAt,
                        )

                        cache.select(CacheEntry.CacheEntryKey(key)).getOrNull().shouldNotBeNull()
                        cache.selectAsync(CacheEntry.CacheEntryKey(key)).getOrNull().shouldNotBeNull()
                        cache
                            .selectAll()
                            .getOrNull()
                            ?.first()
                            .shouldNotBeNull()
                        cache
                            .selectAllAsync()
                            .getOrNull()
                            ?.first()
                            .shouldNotBeNull()

                        delay(8.seconds)

                        Files.deleteIfExists(Path.of("src", "test", "resources", "async", "cache"))
                        Files.deleteIfExists(Path.of("src", "test", "resources", "async", "expirations"))
                        Files.deleteIfExists(Path.of("src", "test", "resources", "async"))
                    }
                }

                `when`("Launch insert the cache") {
                    then("Should be able to insert, select, select all and then expires the cache") {
                        Files.createDirectory(Path.of("src", "test", "resources", "launch"))
                        Files.createDirectory(Path.of("src", "test", "resources", "launch", "cache"))
                        Files.createDirectory(Path.of("src", "test", "resources", "launch", "expirations"))

                        val properties =
                            LocalFileCacheProperties(
                                baseDir = listOf("src", "test", "resources", "launch"),
                            )
                        val cache =
                            LocalFileCache(
                                properties = properties,
                                expirationWorker = LocalFileExpirationWorkerGeneric(),
                                writer = AsynchronousWriterGeneric(),
                                asynchronousReader = AsynchronousReaderGeneric(),
                                reader = ReaderGeneric(),
                            )
                        val key = UUID.randomUUID().toString()
                        val expiresAt = Clock.System.now().plus(5.seconds)

                        cache.launchInsert(
                            entry =
                                CacheEntry(
                                    key = CacheEntry.CacheEntryKey(key),
                                    expiresAt = expiresAt,
                                    payload = "test=payload",
                                ),
                            expiresAt = expiresAt,
                        )

                        cache.select(CacheEntry.CacheEntryKey(key)).shouldNotBeNull()
                        cache.selectAsync(CacheEntry.CacheEntryKey(key)).getOrNull().shouldNotBeNull()
                        cache
                            .selectAll()
                            .getOrNull()
                            ?.first()
                            .shouldNotBeNull()
                        cache
                            .selectAllAsync()
                            .getOrNull()
                            ?.first()
                            .shouldNotBeNull()

                        delay(8.seconds)

                        Files.deleteIfExists(Path.of("src", "test", "resources", "launch", "cache"))
                        Files.deleteIfExists(Path.of("src", "test", "resources", "launch", "expirations"))
                        Files.deleteIfExists(Path.of("src", "test", "resources", "launch"))
                    }
                }
            }
        }
    })
