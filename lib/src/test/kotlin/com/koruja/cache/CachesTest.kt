package com.koruja.cache

import com.koruja.cache.inmemory.InMemoryCache
import com.koruja.cache.inmemory.InMemoryExpirationDeciderGeneric
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class CachesTest : BehaviorSpec({

    context("Structure to handle multiple caches as singleton") {
        given("Two distinct caches") {
            `when`("Insert two caches") {
                then("Should keep in memory") {
                    val key1 = Caches.CachesKey("key1")
                    val inMemoryCache1 = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())
                    val key2 = Caches.CachesKey("key2")
                    val inMemoryCache2 = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())

                    Caches.insert(key = key1, cache = inMemoryCache1)
                    Caches.insert(key = key2, cache = inMemoryCache2)

                    Caches.select(key1).shouldNotBeNull()
                    Caches.select(key2).shouldNotBeNull()
                }
            }

            `when`("Delete one cache") {
                then("Should remain one cache in memory") {
                    val key1 = Caches.CachesKey("key1")
                    val inMemoryCache1 = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())
                    val key2 = Caches.CachesKey("key2")
                    val inMemoryCache2 = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())

                    Caches.insert(key = key1, cache = inMemoryCache1)
                    Caches.insert(key = key2, cache = inMemoryCache2)

                    Caches.select(key1).shouldNotBeNull()
                    Caches.select(key2).shouldNotBeNull()

                    Caches.delete(key1)

                    Caches.select(key1).shouldBeNull()
                    Caches.delete(key2).shouldNotBeNull()
                }
            }

            `when`("Select all caches") {
                then("Should return two caches") {
                    val key1 = Caches.CachesKey("key1")
                    val inMemoryCache1 = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())
                    val key2 = Caches.CachesKey("key2")
                    val inMemoryCache2 = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())

                    Caches.insert(key = key1, cache = inMemoryCache1)
                    Caches.insert(key = key2, cache = inMemoryCache2)

                    Caches.selectAll().size shouldBe 2
                }
            }
        }
    }
})
