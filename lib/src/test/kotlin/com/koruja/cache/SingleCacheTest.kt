package com.koruja.cache

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull

class SingleCacheTest : BehaviorSpec({

    context("Obtain one single instance of a cache") {
        given("A SingleCache instance") {
            `when`("Created") {
                then("Should have one single instance of a cache") {
                    SingleCache.insert(cache = InMemoryCache())
                    SingleCache.select().shouldNotBeNull()
                }
            }
        }
    }
})
