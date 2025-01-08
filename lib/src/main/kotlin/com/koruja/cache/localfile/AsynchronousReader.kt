package com.koruja.cache.localfile

import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

interface AsynchronousReader {
    suspend fun read(filePath: Path, scope: CoroutineScope): Deferred<String>
}