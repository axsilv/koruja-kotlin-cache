package com.koruja.cache.localfile

data class LocalFileCacheProperties(
    val baseDir: List<String>,
    val deleteExpiredCache: Boolean = true
)