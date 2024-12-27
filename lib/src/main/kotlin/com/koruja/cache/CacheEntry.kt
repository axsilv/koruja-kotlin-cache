package com.koruja.cache

import kotlinx.datetime.Instant

data class CacheEntry(
    val id: CacheEntryKey,
    val expiresAt: Instant,
    val payload: String,
) {
    data class CacheEntryKey(private val id: String) {
        override fun toString(): String = id
    }
}
