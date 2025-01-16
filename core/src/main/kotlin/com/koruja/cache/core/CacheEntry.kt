package com.koruja.cache.core

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CacheEntry(
    val key: CacheEntryKey,
    val expiresAt: Instant,
    val payload: String,
) {
    @Serializable
    data class CacheEntryKey(
        private val id: String,
    ) {
        override fun toString(): String = id
    }
}
