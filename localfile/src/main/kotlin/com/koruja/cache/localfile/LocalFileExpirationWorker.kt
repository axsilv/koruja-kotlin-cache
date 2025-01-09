package com.koruja.cache.localfile

import com.koruja.cache.core.ExpirationWorker
import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex

interface LocalFileExpirationWorker : ExpirationWorker {
    suspend fun run(mutex: Mutex, expirationPath: Path, cachePath: Path, scope: CoroutineScope)
}