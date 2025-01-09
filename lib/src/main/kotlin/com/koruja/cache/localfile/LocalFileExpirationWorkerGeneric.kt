package com.koruja.cache.localfile

import com.koruja.cache.LocalFileExpirationWorker
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class LocalFileExpirationWorkerGeneric : LocalFileExpirationWorker {

    @OptIn(ExperimentalPathApi::class)
    override suspend fun run(
        mutex: Mutex,
        expirationPath: Path,
        cachePath: Path,
        scope: CoroutineScope
    ) {
        mutex.withLock {
            expirationPath.toFile().listFiles()
                ?.filter { it.isDirectory }
                ?.map { folder ->
                    launchDelete(scope, folder, cachePath)
                }?.joinAll()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun launchDelete(
        scope: CoroutineScope,
        folder: File,
        cachePath: Path
    ) = scope.launch {
        val folderInstant = folder.toInstant()

        if (folderInstant.isExpired()) {
            folder.listFiles()
                ?.filter { it.isFile }
                ?.forEach { file ->
                    val cacheLocation = cachePath.resolve(file.name)
                    runCatching {
                        cacheLocation.deleteRecursively()
                    }
                }

            runCatching {
                folder.deleteRecursively()
            }
        }
    }

    private fun File.toInstant() = Instant.parse(this.name.replace("_", ":"))

    private fun Instant.isExpired() = this <= Clock.System.now()
}
