package com.koruja.cache.localfile

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

class AsynchronousReaderGeneric : AsynchronousReader {

    override suspend fun read(
        filePath: Path,
        scope: CoroutineScope
    ) = scope.async {
        val channel = AsynchronousFileChannel.open(filePath, READ)
        val buffer = ByteBuffer.allocate(channel.size().toInt())
        channel.read(buffer, 0).get()
        channel.close()
        buffer.flip()
        String(buffer.array())
    }
}
