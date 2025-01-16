package com.koruja.cache.localfile

import com.koruja.cache.core.ExpirationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import java.nio.file.Path

interface LocalFileExpirationWorker : ExpirationWorker {
    suspend fun run(
        mutex: Mutex,
        expirationPath: Path,
        cachePath: Path,
        scope: CoroutineScope,
    )
}
