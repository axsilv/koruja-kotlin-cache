package com.koruja.cache

import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex

interface ExpirationWorker

interface LocalFileExpirationWorker : ExpirationWorker {
    suspend fun run(mutex: Mutex, expirationPath: Path, cachePath: Path, scope: CoroutineScope)
}
