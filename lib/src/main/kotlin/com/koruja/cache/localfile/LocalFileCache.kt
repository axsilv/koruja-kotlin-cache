package com.koruja.cache.localfile

import com.koruja.cache.Cache
import com.koruja.cache.CacheEntry
import com.koruja.cache.CacheEntry.CacheEntryKey
import com.koruja.cache.CacheException.CacheAlreadyPersisted
import com.koruja.cache.inmemory.InMemoryCache
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

class LocalFileCache(
    properties: LocalFileCacheProperties
) : Cache {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val cachePath: String = "${properties.baseDir}${File.separator}cache${File.separator}"
    private val expirationPath: String = "${properties.baseDir}${File.separator}expirations${File.separator}"
    private val inMemoryCache: InMemoryCache = InMemoryCache()

    private fun writeFileAsync(filePath: Path, content: String): Deferred<Unit> = scope.async {
        asynchronousWriter(filePath = filePath, content = content)
    }

    private fun launchWriteFile(filePath: Path, content: String): Job = scope.launch {
        asynchronousWriter(filePath = filePath, content = content)
    }

    private fun asynchronousWriter(filePath: Path, content: String) {
        val channel = AsynchronousFileChannel.open(filePath, WRITE, CREATE)
        val buffer = ByteBuffer.wrap(content.toByteArray())
        channel.write(buffer, 0).get()
        channel.close()
    }

    private fun writeFileSync(filePath: String, content: String) {
        File(filePath).writeText(content)
    }

    private fun readFileAsync(filePath: Path): Deferred<String> = scope.async {
        asynchronousReader(filePath = filePath)
    }

    private fun readFileSync(filePath: Path): String {
        return asynchronousReader(filePath = filePath)
    }

    private fun asynchronousReader(filePath: Path): String {
        val channel = AsynchronousFileChannel.open(filePath, READ)
        val buffer = ByteBuffer.allocate(channel.size().toInt())
        channel.read(buffer, 0).get()
        channel.close()
        buffer.flip()
        return String(buffer.array())
    }

    override fun insert(entry: CacheEntry, expiresAt: Instant): Result<Unit> = runCatching {
        select(key = entry.id).getOrNull()?.let {
            throw CacheAlreadyPersisted()
        }

        writeFileSync(
            filePath = cachePath + entry.id,
            content = Json.encodeToString(entry)
        )

        inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)
    }

    override fun insertAsync(entry: CacheEntry, expiresAt: Instant): Result<Deferred<Unit>> = runCatching {
        select(key = entry.id).getOrNull()?.let {
            throw CacheAlreadyPersisted()
        }

        val deferred = writeFileAsync(
            filePath = Path.of(URI.create(cachePath + entry.id)),
            content = Json.encodeToString(entry)
        )

        inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)

        deferred
    }


    override fun launchInsert(entry: CacheEntry, expiresAt: Instant): Job = scope.launch {
        select(key = entry.id).getOrNull()?.let {
            throw CacheAlreadyPersisted()
        }

        launchWriteFile(
            filePath = Path.of(URI.create(cachePath + entry.id)),
            content = Json.encodeToString(entry)
        )

        inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)
    }

    override fun select(key: CacheEntryKey): Result<CacheEntry?> = runCatching {
        val file = readFileSync(Path.of(URI.create(cachePath + key)))
        Json.decodeFromString<CacheEntry>(file)
    }

    override fun selectAsync(key: CacheEntryKey): Deferred<Result<CacheEntry?>> = scope.async {
        runCatching {
            val file = readFileAsync(Path.of(URI.create(cachePath + key))).await()
            Json.decodeFromString<CacheEntry>(file)
        }
    }

    override fun selectAll(): Result<List<CacheEntry>> {
        TODO("Not yet implemented")
    }
}