package com.koruja.cache.localfile

import com.koruja.cache.Cache
import com.koruja.cache.CacheEntry
import com.koruja.cache.CacheEntry.CacheEntryKey
import com.koruja.cache.CacheException.CacheAlreadyPersisted
import com.koruja.cache.expiration.LocalFileExpirationWorker
import com.koruja.cache.inmemory.InMemoryCache
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.READ
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

class LocalFileCache(
    private val properties: LocalFileCacheProperties,
    private val expirationWorker: LocalFileExpirationWorker,
    private val writer: AsynchronousWriter
) : Cache {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val cachePath: Path =
        Paths.get("", *properties.baseDir.toTypedArray(), "cache")
    private val expirationPath: Path =
        Paths.get("", *properties.baseDir.toTypedArray(), "expirations")
    private val inMemoryCache: InMemoryCache = InMemoryCache()
    private val mutex: Mutex = Mutex()

    init {
        if (Files.notExists(cachePath)) {
            Files.createDirectory(cachePath)
        }

        if (Files.notExists(cachePath)) {
            Files.createDirectory(expirationPath)
        }

        if (properties.deleteExpiredCache) {
            scope.launch { expirationWorker() }
        }
    }

    private fun writeFileAsync(filePath: Path, content: String): Deferred<Unit> = scope.async {
        asynchronousWriter(filePath = filePath, content = content)
    }

    private fun launchWriteFile(filePath: Path, content: String): Job = scope.launch {
        asynchronousWriter(filePath = filePath, content = content)
    }

    private suspend fun asynchronousWriter(filePath: Path, content: String) = writer.write(
        mutex = mutex,
        filePath = filePath,
        content = content,
        scope = scope
    )


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

    override suspend fun insert(entry: CacheEntry, expiresAt: Instant) {
        prepareCache(entry, expiresAt)

        writeFileAsync(
            filePath = cachePath.resolve("${entry.id}.txt"),
            content = Json.encodeToString(entry)
        ).await()

        inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)
    }

    private fun expiresFileContent(entry: CacheEntry, expiresAt: Instant) = Json.encodeToString(
        mapOf(
            "key" to entry.id.toString(),
            "expiresAt" to expiresAt.toString()
        )
    )

    private fun expiresFilePath(expiresAt: Instant, entry: CacheEntry): Path {
        val expiresFolder = expirationPath.resolve(expiresAt.toString().replace(":", "_"))

        if (Files.notExists(expiresFolder)) {
            Files.createDirectory(expiresFolder)
        }

        return expiresFolder.resolve("${entry.id}.txt")
    }

    override suspend fun insertAsync(entry: CacheEntry, expiresAt: Instant): Deferred<Unit> {
        prepareCache(entry, expiresAt)

        val deferred = writeFileAsync(
            filePath = cachePath.resolve(entry.id.toString()),
            content = Json.encodeToString(entry)
        )

        inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)

        return deferred
    }


    override suspend fun launchInsert(entry: CacheEntry, expiresAt: Instant): Job = scope.launch {
        prepareCache(entry, expiresAt)

        launchWriteFile(
            filePath = cachePath.resolve(entry.id.toString()),
            content = Json.encodeToString(entry)
        )

        inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)
    }

    private suspend fun LocalFileCache.prepareCache(
        entry: CacheEntry,
        expiresAt: Instant
    ) {
        select(key = entry.id)?.let {
            throw CacheAlreadyPersisted()
        }

        if (properties.deleteExpiredCache) {
            writeFileAsync(
                filePath = expiresFilePath(expiresAt = expiresAt, entry = entry),
                content = expiresFileContent(entry = entry, expiresAt = expiresAt)
            ).await()
        }
    }

    override suspend fun select(key: CacheEntryKey): CacheEntry? {
        val memoryEntry = inMemoryCache.select(key = key)

        if (memoryEntry != null) {
            return memoryEntry
        }

        val filePath = cachePath.resolve("$key.txt")
        if (Files.notExists(filePath)) {
            return null
        }

        val file = readFileSync(filePath)
        return Json.decodeFromString<CacheEntry>(file)
    }

    override suspend fun selectAsync(key: CacheEntryKey): Deferred<CacheEntry?> = scope.async {
        val file = readFileAsync(cachePath.resolve(key.toString())).await()
        Json.decodeFromString<CacheEntry>(file)
    }

    override suspend fun selectAll(): List<CacheEntry> = readAllFilesSync().map { file ->
        try {
            val lines = file.readText()
            return listOf(Json.decodeFromString<CacheEntry>(lines))
        } catch (e: FileNotFoundException) {
            return emptyList()
        }
    }

    override suspend fun selectAllAsync(): Deferred<List<CacheEntry>> = scope.async {
        readAllFilesAsync().await()
            .map { file ->
                val lines = file.readText()
                Json.decodeFromString<CacheEntry>(lines)
            }
    }

    override suspend fun cleanAll(): Deferred<Unit> = scope.async {
        Files.deleteIfExists(cachePath)
        Files.deleteIfExists(expirationPath)
    }

    private fun readAllFilesAsync(): Deferred<List<File>> = scope.async {
        readAllFiles().toList()
    }


    private fun readAllFilesSync(): List<File> = readAllFiles().toList()


    private fun readAllFiles(): MutableList<File> {
        val fileList = mutableListOf<File>()

        Files.walk(cachePath).use { stream ->
            stream.filter {
                Files.isRegularFile(it)
            }.forEach { path ->
                fileList.add(path.toFile())
            }
        }

        return fileList
    }

    private suspend fun expirationWorker() {
        while (true) {
            expirationWorker.run(
                mutex = mutex,
                expirationPath = expirationPath,
                cachePath = cachePath,
                scope = scope
            )
        }
    }
}