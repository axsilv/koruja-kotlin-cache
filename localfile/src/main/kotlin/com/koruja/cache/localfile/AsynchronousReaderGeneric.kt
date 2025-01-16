package com.koruja.cache.localfile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ

class AsynchronousReaderGeneric : AsynchronousReader {
    override suspend fun read(
        filePath: Path,
        scope: CoroutineScope,
    ) = scope.async {
        val channel = AsynchronousFileChannel.open(filePath, READ)
        try {
            val buffer = ByteBuffer.allocate(channel.size().toInt())
            channel.read(buffer, 0).get()
            buffer.flip()
            String(buffer.array())
        } finally {
            channel.close()
        }
    }
}
