package com.koruja.cache

import kotlinx.datetime.Instant

data class CacheEntry(
    val id: CacheEntryId,
    val expiresAt: Instant,
    val payload: ByteArray
) {
    data class CacheEntryId(private val id: String) {
        override fun toString(): String = id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CacheEntry

        if (id != other.id) return false
        if (expiresAt != other.expiresAt) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + expiresAt.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
