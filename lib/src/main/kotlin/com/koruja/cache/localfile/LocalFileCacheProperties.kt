package com.koruja.cache.localfile

data class LocalFileCacheProperties(
    val baseDir: String,
    val keepExpiredCache: Boolean = false
)