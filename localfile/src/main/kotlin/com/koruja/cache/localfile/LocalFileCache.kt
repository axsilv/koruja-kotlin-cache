package com.koruja.cache.localfile

import com.koruja.cache.core.Cache
import com.koruja.cache.core.CacheEntry
import com.koruja.cache.core.CacheEntry.CacheEntryKey
import com.koruja.cache.core.CacheException
import com.koruja.cache.core.CacheException.CacheAlreadyPersisted
import com.koruja.cache.core.Decorator
import com.koruja.cache.inmemory.InMemoryCache
import com.koruja.cache.inmemory.InMemoryExpirationDecider
import com.koruja.cache.inmemory.InMemoryExpirationDeciderGeneric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class LocalFileCache(
    private val properties: LocalFileCacheProperties,
    private val expirationWorker: LocalFileExpirationWorker = LocalFileExpirationWorkerGeneric(),
    private val writer: AsynchronousWriter = AsynchronousWriterGeneric(),
    private val asynchronousReader: AsynchronousReader = AsynchronousReaderGeneric(),
    private val reader: Reader = ReaderGeneric(),
    private val expirationDecider: InMemoryExpirationDecider = InMemoryExpirationDeciderGeneric(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val decorators: List<Decorator> = emptyList(),
) : Cache {
    private val cachePath: Path =
        Paths.get("", *properties.baseDir.toTypedArray(), "cache")
    private val expirationPath: Path =
        Paths.get("", *properties.baseDir.toTypedArray(), "expirations")
    private val inMemoryCache: InMemoryCache =
        InMemoryCache(
            expirationDecider = expirationDecider,
            insertDecorators = emptyList(),
        )
    private val mutex: Mutex = Mutex()

    init {
        if (Files.notExists(cachePath) || Files.notExists(expirationPath)) {
            throw CacheException.StartUpFailure("One ore more needed files path do not exists")
        }

        if (properties.deleteExpiredCache) {
            scope.launch { expirationWorker() }
        }
    }

    private fun writeFileAsync(
        filePath: Path,
        content: String,
    ): Deferred<Unit> =
        scope.async {
            asynchronousWriter(filePath = filePath, content = content)
        }

    private fun launchWriteFile(
        filePath: Path,
        content: String,
    ): Job =
        scope.launch {
            asynchronousWriter(filePath = filePath, content = content)
        }

    private suspend fun asynchronousWriter(
        filePath: Path,
        content: String,
    ) = writer.write(
        mutex = mutex,
        filePath = filePath,
        content = content,
        scope = scope,
    )

    private suspend fun readFileAsync(filePath: Path): String =
        scope
            .async {
                asynchronousReader(filePath = filePath)
            }.await()

    private fun readFileSync(filePath: Path): String = reader.read(filePath = filePath)

    private suspend fun asynchronousReader(filePath: Path): String =
        asynchronousReader
            .read(
                filePath = filePath,
                scope = scope,
            ).await()

    override suspend fun insert(
        entry: CacheEntry,
        expiresAt: Instant,
    ) = runCatching {
        prepareCache(entry, expiresAt)

        writeFileAsync(
            filePath = cachePath.resolve("${entry.key}${properties.fileType.fileFormat}"),
            content = Json.encodeToString(entry),
        ).await()

        if (properties.enableInMemoryCacheSupport) {
            inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)
        }
    }

    private fun expiresFileContent(
        entry: CacheEntry,
        expiresAt: Instant,
    ) = Json.encodeToString(
        mapOf(
            "key" to entry.key.toString(),
            "expiresAt" to expiresAt.toString(),
        ),
    )

    private fun expiresFilePath(
        expiresAt: Instant,
        entry: CacheEntry,
    ): Path {
        val expiresFolder = expirationPath.resolve(expiresAt.toString().replace(":", "_"))

        if (Files.notExists(expiresFolder)) {
            Files.createDirectory(expiresFolder)
        }

        return expiresFolder.resolve("${entry.key}${properties.fileType.fileFormat}")
    }

    override suspend fun insertAsync(
        entry: CacheEntry,
        expiresAt: Instant,
    ) = runCatching {
        prepareCache(entry, expiresAt)

        val deferred =
            writeFileAsync(
                filePath = cachePath.resolve(entry.key.toString() + properties.fileType.fileFormat),
                content = Json.encodeToString(entry),
            )

        if (properties.enableInMemoryCacheSupport) {
            inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)
        }

        deferred.await()
    }

    override suspend fun launchInsert(
        entry: CacheEntry,
        expiresAt: Instant,
    ) = runCatching {
        prepareCache(entry, expiresAt)

        val job =
            launchWriteFile(
                filePath = cachePath.resolve(entry.key.toString() + properties.fileType.fileFormat),
                content = Json.encodeToString(entry),
            )

        if (properties.enableInMemoryCacheSupport) {
            inMemoryCache.launchInsert(entry = entry, expiresAt = expiresAt)
        }

        job.join()
    }

    private suspend fun LocalFileCache.prepareCache(
        entry: CacheEntry,
        expiresAt: Instant,
    ) {
        select(key = entry.key).let { result ->
            val throwable = result.exceptionOrNull()
            if (result.isFailure && throwable != null) {
                throw throwable
            }

            if (result.isSuccess && result.getOrNull() != null) {
                throw CacheAlreadyPersisted()
            }
        }

        if (properties.deleteExpiredCache) {
            writeFileAsync(
                filePath = expiresFilePath(expiresAt = expiresAt, entry = entry),
                content = expiresFileContent(entry = entry, expiresAt = expiresAt),
            ).await()
        }
    }

    override suspend fun select(key: CacheEntryKey): Result<CacheEntry?> =
        runCatching {
            val memoryEntry =
                if (properties.enableInMemoryCacheSupport) {
                    inMemoryCache.select(key = key).getOrNull()
                } else {
                    null
                }

            if (memoryEntry == null) {
                val filePath = cachePath.resolve("$key${properties.fileType.fileFormat}")

                if (Files.notExists(filePath)) {
                    null
                } else {
                    val file = readFileSync(filePath)
                    Json.decodeFromString<CacheEntry>(file)
                }
            } else {
                memoryEntry
            }
        }

    override suspend fun selectAsync(key: CacheEntryKey) =
        runCatching {
            scope
                .async {
                    val memoryEntry =
                        if (properties.enableInMemoryCacheSupport) {
                            inMemoryCache.selectAsync(key = key).getOrNull()
                        } else {
                            null
                        }

                    if (memoryEntry == null) {
                        val file = readFileAsync(cachePath.resolve("$key${properties.fileType.fileFormat}"))
                        Json.decodeFromString<CacheEntry>(file)
                    } else {
                        memoryEntry
                    }
                }.await()
        }

    override suspend fun selectAll() =
        runCatching {
            readAllFilesSync().mapNotNull { file ->
                try {
                    val lines = file.readText()
                    Json.decodeFromString<CacheEntry>(lines)
                } catch (_: FileNotFoundException) {
                    null
                }
            }
        }

    override suspend fun selectAllAsync() =
        runCatching {
            scope
                .async {
                    readAllFilesAsync()
                        .await()
                        .map { file ->
                            scope.async {
                                val lines = file.readText()
                                Json.decodeFromString<CacheEntry>(lines)
                            }
                        }.awaitAll()
                }.await()
        }

    override suspend fun cleanAll() =
        runCatching {
            scope
                .launch {
                    val jobs = mutableListOf<Job>()

                    jobs +=
                        Files.walk(cachePath).use { stream ->
                            stream
                                .filter { Files.isRegularFile(it) }
                                .map {
                                    scope.launch {
                                        runCatching {
                                            val cacheLocation = cachePath.resolve(it)
                                            try {
                                                mutex.lock(cacheLocation)
                                                cacheLocation.toFile().deleteRecursively()
                                            } finally {
                                                mutex.unlock(cacheLocation)
                                            }
                                        }
                                    }
                                }.toList()
                        }

                    jobs +=
                        Files.walk(expirationPath).use { stream ->
                            stream
                                .filter { Files.isRegularFile(it) }
                                .map {
                                    scope.launch {
                                        runCatching {
                                            val cacheLocation = expirationPath.resolve(it)
                                            try {
                                                mutex.lock(cacheLocation)
                                                cacheLocation.toFile().deleteRecursively()
                                            } finally {
                                                mutex.unlock(cacheLocation)
                                            }
                                        }
                                    }
                                }.toList()
                        }

                    jobs.joinAll()
                }.join()
        }

    private fun readAllFilesAsync(): Deferred<List<File>> =
        scope.async {
            readAllFiles().toList()
        }

    private fun readAllFilesSync(): List<File> = readAllFiles().toList()

    private fun readAllFiles(): MutableList<File> {
        val fileList = mutableListOf<File>()

        Files.walk(cachePath).use { stream ->
            stream
                .filter {
                    Files.isRegularFile(it)
                }.forEach { path ->
                    fileList.add(path.toFile())
                }
        }

        return fileList
    }

    private suspend fun expirationWorker() {
        while (true) {
            runCatching {
                expirationWorker.run(
                    mutex = mutex,
                    expirationPath = expirationPath,
                    cachePath = cachePath,
                    scope = scope,
                )
            }
        }
    }
}
