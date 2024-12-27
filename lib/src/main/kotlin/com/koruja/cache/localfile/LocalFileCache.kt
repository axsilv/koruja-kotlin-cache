package com.koruja.cache.localfile

import com.koruja.cache.Cache
import com.koruja.cache.CacheEntry
import com.koruja.cache.CacheEntry.CacheEntryKey
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

    private val cachePath = "${properties.baseDir}${File.separator}cache${File.separator}"
    private val expirationPath = "${properties.baseDir}${File.separator}expirations${File.separator}"

    private fun writeFileAsync(filePath: Path, content: String): Deferred<Unit> = scope.async {
        val channel = AsynchronousFileChannel.open(filePath, WRITE, CREATE)
        val buffer = ByteBuffer.wrap(content.toByteArray())
        channel.write(buffer, 0).get()
        channel.close()
    }

    private fun writeFileSync(filePath: String, content: String) {
        File(filePath).writeText(content)
    }

    private fun readFileAsync(filePath: Path): Deferred<String> = scope.async {
        val channel = AsynchronousFileChannel.open(filePath, READ)
        val buffer = ByteBuffer.allocate(channel.size().toInt())
        channel.read(buffer, 0).get()
        channel.close()
        buffer.flip()
        String(buffer.array())
    }

    private fun readFileSync(filePath: Path): String {
        val channel = AsynchronousFileChannel.open(filePath, READ)
        val buffer = ByteBuffer.allocate(channel.size().toInt())
        channel.read(buffer, 0).get()
        channel.close()
        buffer.flip()
        return String(buffer.array())
    }

    override fun insert(entry: CacheEntry, expiresAt: Instant): Result<Unit> = runCatching {
        writeFileSync(
            filePath = cachePath + entry.id,
            content = Json.encodeToString(entry)
        )
    }

    override fun insertAsync(entry: CacheEntry, expiresAt: Instant): Deferred<Unit> = writeFileAsync(
        filePath = Path.of(URI.create(cachePath + entry.id)),
        content = Json.encodeToString(entry)
    )


    override fun launchInsert(entry: CacheEntry, expiresAt: Instant): Job = scope.launch {
        writeFileAsync(
            filePath = Path.of(URI.create(cachePath + entry.id)),
            content = Json.encodeToString(entry)
        ).await()
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