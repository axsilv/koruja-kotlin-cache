package com.koruja.cache.localfile

import java.nio.file.Path

interface Reader {
    fun read(filePath: Path): String
}
