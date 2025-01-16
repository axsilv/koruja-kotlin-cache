package com.koruja.cache.core

import java.util.concurrent.ConcurrentHashMap

object Caches {
    private val caches: ConcurrentHashMap<CachesKey, Cache> = ConcurrentHashMap()

    fun insert(
        key: CachesKey,
        cache: Cache,
    ) = caches.put(key, cache)

    fun delete(key: CachesKey) = caches.remove(key)

    fun select(key: CachesKey) = caches[key]

    fun selectAll() = caches.values.toList()

    data class CachesKey(
        val key: String,
    ) {
        override fun toString(): String = key
    }
}
