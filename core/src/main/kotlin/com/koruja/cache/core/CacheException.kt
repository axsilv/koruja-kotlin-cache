package com.koruja.cache.core

sealed class CacheException(
    message: String,
) : Exception(message) {
    class CacheAlreadyPersisted :
        CacheException(
            message = "The given cache entry has already been persisted inside the cache",
        )

    class StartUpFailure(
        message: String,
    ) : CacheException(message = message)
}
