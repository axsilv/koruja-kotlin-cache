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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

class LocalFileCache(
    properties: LocalFileCacheProperties
) : Cache {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val cachePath: String = "${properties.baseDir}${File.separator}cache${File.separator}"
    private val expirationPath: String = "${properties.baseDir}${File.separator}expirations${File.separator}"
    private val inMemoryCache: InMemoryCache = InMemoryCache()
    private val mutex: Mutex = Mutex()

    init {
        if (properties.keepExpiredCache) {
            scope.launch { expirationWorker() }
        }
    }

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

    override suspend fun insert(entry: CacheEntry, expiresAt: Instant) {
        select(key = entry.id)?.let {
            throw CacheAlreadyPersisted()
        }

        writeFileSync(
            filePath = expiresFilePath(expiresAt = expiresAt, entry = entry),
            content = expiresFileContent(entry = entry, expiresAt = expiresAt)
        )

        writeFileSync(
            filePath = cachePath + entry.id,
            content = Json.encodeToString(entry)
        )

        inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)
    }

    private fun expiresFileContent(entry: CacheEntry, expiresAt: Instant) = Json.encodeToString(
        mapOf(
            "key" to entry.id.toString(),
            "expiresAt" to expiresAt.toString()
        )
    )

    private fun expiresFilePath(expiresAt: Instant, entry: CacheEntry) =
        "$expirationPath${File.separator}$expiresAt${File.separator}${entry.id}"

    override fun insertAsync(entry: CacheEntry, expiresAt: Instant): Deferred<Unit> {
        select(key = entry.id)?.let {
            throw CacheAlreadyPersisted()
        }

        writeFileSync(
            filePath = expiresFilePath(expiresAt = expiresAt, entry = entry),
            content = expiresFileContent(entry = entry, expiresAt = expiresAt)
        )

        val deferred = writeFileAsync(
            filePath = Path.of(URI.create(cachePath + entry.id)),
            content = Json.encodeToString(entry)
        )

        inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)

        return deferred
    }


    override fun launchInsert(entry: CacheEntry, expiresAt: Instant): Job = scope.launch {
        select(key = entry.id)?.let {
            throw CacheAlreadyPersisted()
        }

        writeFileSync(
            filePath = expiresFilePath(expiresAt = expiresAt, entry = entry),
            content = expiresFileContent(entry = entry, expiresAt = expiresAt)
        )

        launchWriteFile(
            filePath = Path.of(URI.create(cachePath + entry.id)),
            content = Json.encodeToString(entry)
        )

        inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)
    }

    override fun select(key: CacheEntryKey): CacheEntry? {
        val memoryEntry = inMemoryCache.select(key = key)

        if (memoryEntry != null) {
            return memoryEntry
        }

        val file = readFileSync(Path.of(URI.create(cachePath + key)))
        return Json.decodeFromString<CacheEntry>(file)
    }

    override fun selectAsync(key: CacheEntryKey): Deferred<CacheEntry?> = scope.async {
        val file = readFileAsync(Path.of(URI.create(cachePath + key))).await()
        Json.decodeFromString<CacheEntry>(file)
    }

    override fun selectAll(): List<CacheEntry> = readAllFilesSync(cachePath).map { file ->
        val lines = file.readText()
        Json.decodeFromString<CacheEntry>(lines)
    }

    override suspend fun selectAllAsync(): Deferred<List<CacheEntry>> = scope.async {
        readAllFilesAsync(cachePath).await()
            .map { file ->
                val lines = file.readText()
                Json.decodeFromString<CacheEntry>(lines)
            }
    }

    private fun readAllFilesAsync(folderPath: String): Deferred<List<File>> = scope.async {
        readAllFiles(folderPath).toList()
    }


    private fun readAllFilesSync(folderPath: String): List<File> = readAllFiles(folderPath).toList()


    private fun readAllFiles(folderPath: String): MutableList<File> {
        val folder = Paths.get(folderPath)
        val fileList = mutableListOf<File>()

        Files.walk(folder).use { stream ->
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
            mutex.withLock {
                File(expirationPath).listFiles()
                    ?.filter { it.isDirectory }
                    ?.map { folder ->
                        scope.async {
                            val folderInstant = Instant.parse(folder.name)
                            if (folderInstant <= Clock.System.now()) {
                                folder?.listFiles()
                                    ?.filter { it.isFile }
                                    ?.forEach {
                                        File(cachePath + it.name).deleteRecursively()
                                    }

                                folder.deleteRecursively()
                            }
                        }
                    }?.awaitAll()
            }
        }
    }
}