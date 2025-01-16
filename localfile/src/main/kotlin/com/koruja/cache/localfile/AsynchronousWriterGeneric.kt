package com.koruja.cache.localfile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE

class AsynchronousWriterGeneric : AsynchronousWriter {
    override suspend fun write(
        mutex: Mutex,
        filePath: Path,
        content: String,
        scope: CoroutineScope,
    ) = try {
        mutex.lock(filePath)

        scope
            .async {
                if (Files.notExists(filePath)) {
                    Files.createFile(filePath)
                }

                val channel = AsynchronousFileChannel.open(filePath, WRITE, CREATE)
                val buffer = ByteBuffer.wrap(content.toByteArray())
                channel.write(buffer, 0).get()
                channel.close()
            }.await()
    } finally {
        mutex.unlock(filePath)
    }
}
