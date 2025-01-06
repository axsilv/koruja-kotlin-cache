package com.koruja.cache.localfile

import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex

interface AsynchronousWriter {
    suspend fun write(mutex: Mutex, filePath: Path, content: String, scope: CoroutineScope)
}
