package com.koruja.cache.localfile

import com.koruja.cache.Cache
import com.koruja.cache.CacheEntry
import com.koruja.cache.CacheEntry.CacheEntryKey
import com.koruja.cache.CacheException.CacheAlreadyPersisted
import com.koruja.cache.LocalFileExpirationWorker
import com.koruja.cache.inmemory.InMemoryCache
import com.koruja.cache.inmemory.InMemoryExpirationDeciderGeneric
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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
    private val writer: AsynchronousWriter,
    private val asynchronousReader: AsynchronousReader,
    private val reader: Reader
) : Cache {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val cachePath: Path =
        Paths.get("", *properties.baseDir.toTypedArray(), "cache")
    private val expirationPath: Path =
        Paths.get("", *properties.baseDir.toTypedArray(), "expirations")
    private val inMemoryCache: InMemoryCache = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())
    private val mutex: Mutex = Mutex()

    init {
        if (Files.notExists(cachePath) || Files.notExists(cachePath)) {
            throw Exception()
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

    private fun readFileSync(filePath: Path): String = reader.read(filePath = filePath)

    private suspend fun asynchronousReader(filePath: Path): String = asynchronousReader.read(
        filePath = filePath,
        scope = scope
    ).await()

    override suspend fun insert(entry: CacheEntry, expiresAt: Instant) {
        prepareCache(entry, expiresAt)

        writeFileAsync(
            filePath = cachePath.resolve("${entry.key}.txt"),
            content = Json.encodeToString(entry)
        ).await()

        inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)
    }

    private fun expiresFileContent(entry: CacheEntry, expiresAt: Instant) = Json.encodeToString(
        mapOf(
            "key" to entry.key.toString(),
            "expiresAt" to expiresAt.toString()
        )
    )

    private fun expiresFilePath(expiresAt: Instant, entry: CacheEntry): Path {
        val expiresFolder = expirationPath.resolve(expiresAt.toString().replace(":", "_"))

        if (Files.notExists(expiresFolder)) {
            Files.createDirectory(expiresFolder)
        }

        return expiresFolder.resolve("${entry.key}.txt")
    }

    override suspend fun insertAsync(entry: CacheEntry, expiresAt: Instant): Deferred<Unit> {
        prepareCache(entry, expiresAt)

        val deferred = writeFileAsync(
            filePath = cachePath.resolve(entry.key.toString() + ".txt"),
            content = Json.encodeToString(entry)
        )

        inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)

        return deferred
    }


    override suspend fun launchInsert(entry: CacheEntry, expiresAt: Instant): Job {
        prepareCache(entry, expiresAt)

        val job = launchWriteFile(
            filePath = cachePath.resolve(entry.key.toString() + ".txt"),
            content = Json.encodeToString(entry)
        )

        inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)

        return job
    }

    private suspend fun LocalFileCache.prepareCache(
        entry: CacheEntry,
        expiresAt: Instant
    ) {
        select(key = entry.key)?.let {
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
        val file = readFileAsync(cachePath.resolve("$key.txt")).await()
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