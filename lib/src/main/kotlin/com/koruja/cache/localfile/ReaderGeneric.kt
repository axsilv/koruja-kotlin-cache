package com.koruja.cache.localfile

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ

class ReaderGeneric : Reader {
    override fun read(filePath: Path): String {
        val channel = AsynchronousFileChannel.open(filePath, READ)
        val buffer = ByteBuffer.allocate(channel.size().toInt())
        channel.read(buffer, 0).get()
        channel.close()
        buffer.flip()
        return String(buffer.array())
    }
}
