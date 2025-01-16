package com.koruja.cache.localfile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import java.nio.file.Path

interface AsynchronousReader {
    suspend fun read(
        filePath: Path,
        scope: CoroutineScope,
    ): Deferred<String>
}
