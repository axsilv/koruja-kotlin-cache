package com.koruja.cache

data class CacheKey(
    val name: String,
    val metadata: Map<String, String>
)
