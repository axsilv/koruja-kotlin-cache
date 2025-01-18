package com.koruja.kotlin.cache.decorators

import com.koruja.cache.core.CacheProperties
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class LoggingDecoratorTest :
    StringSpec({

        "should log when debugging is enabled" {
            val properties = CacheProperties(isCacheDebugEnabled = true)
            val decorator = LoggingDecorator(properties)
            val mockFunction: suspend () -> String = mockk()
            coEvery { mockFunction() } returns "result"

            val result = decorator.decorate("test", mockFunction)

            result shouldBe "result"

            coVerify { mockFunction() }
            confirmVerified(mockFunction)
        }

        "should not log when debugging is disabled" {
            val properties = CacheProperties(isCacheDebugEnabled = false)
            val decorator = LoggingDecorator(properties)

            val mockFunction: suspend () -> String = mockk()
            coEvery { mockFunction() } returns "result"

            val result =
                runBlocking {
                    decorator.decorate("test", mockFunction)
                }

            result shouldBe "result"

            coVerify { mockFunction() }
            confirmVerified(mockFunction)
        }
    })
