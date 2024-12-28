package com.koruja.cache

sealed class CacheException(message: String) : Exception(message) {
    class CacheAlreadyPersisted : CacheException(
        "The given cache entry has already been persisted inside the cache"
    )
}