package com.koruja.cache.localfile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import java.nio.file.Path

interface AsynchronousWriter {
    suspend fun write(
        mutex: Mutex,
        filePath: Path,
        content: String,
        scope: CoroutineScope,
    )
}
