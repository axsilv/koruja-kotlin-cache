package com.koruja.cache.expiration

import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class LocalFileExpirationWorkerGeneric : LocalFileExpirationWorker {
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
                    scope.async {
                        val folderInstant = Instant.parse(folder.name.replace("_", ":"))
                        if (folderInstant <= Clock.System.now()) {
                            folder?.listFiles()
                                ?.filter { it.isFile }
                                ?.forEach {
                                    cachePath.resolve(it.name).toFile().deleteRecursively()
                                }

                            folder.deleteRecursively()
                        }
                    }
                }?.awaitAll()
        }
    }
}
