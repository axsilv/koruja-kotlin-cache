package com.koruja.cache.localfile

import java.io.File
import java.nio.file.Path

class ReaderGeneric : Reader {
    override fun read(filePath: Path): String = File(filePath.toUri()).readText()
}
