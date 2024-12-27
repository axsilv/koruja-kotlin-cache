package com.koruja.cache

object SingleCache {
    private val cache: Cache = Cache()

    fun select(): Cache = cache
}