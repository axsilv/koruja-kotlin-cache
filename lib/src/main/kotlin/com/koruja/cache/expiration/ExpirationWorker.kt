package com.koruja.cache.expiration

import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex

sealed interface ExpirationWorker

interface LocalFileExpirationWorker : ExpirationWorker {
    suspend fun run(mutex: Mutex, expirationPath: Path, cachePath: Path, scope: CoroutineScope)
}
