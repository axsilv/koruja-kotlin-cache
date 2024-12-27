package com.koruja.cache

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
                    val cache1 = Cache()
                    val key2 = Caches.CachesKey("key2")
                    val cache2 = Cache()

                    Caches.insert(key = key1, cache = cache1)
                    Caches.insert(key = key2, cache = cache2)

                    Caches.select(key1).shouldNotBeNull()
                    Caches.select(key2).shouldNotBeNull()
                }
            }

            `when`("Delete one cache") {
                then("Should remain one cache in memory") {
                    val key1 = Caches.CachesKey("key1")
                    val cache1 = Cache()
                    val key2 = Caches.CachesKey("key2")
                    val cache2 = Cache()

                    Caches.insert(key = key1, cache = cache1)
                    Caches.insert(key = key2, cache = cache2)

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
                    val cache1 = Cache()
                    val key2 = Caches.CachesKey("key2")
                    val cache2 = Cache()

                    Caches.insert(key = key1, cache = cache1)
                    Caches.insert(key = key2, cache = cache2)

                    Caches.selectAll().size shouldBe 2
                }
            }
        }
    }
})
