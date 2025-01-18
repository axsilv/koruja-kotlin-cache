package com.koruja.kotlin.cache.decorators

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class WithTimeoutDecoratorTest :
    StringSpec({

        "should complete the function within the timeout" {
            val timeoutMillis = 1000L
            val decorator = WithTimeoutDecorator(timeoutMillis)

            val mockFunction: suspend () -> String = {
                delay(500)
                "result"
            }

            val result =
                runBlocking {
                    decorator.decorate("test", mockFunction)
                }

            result shouldBe "result"
        }

        "should throw TimeoutCancellationException when the function exceeds the timeout" {
            val timeoutMillis = 1000L
            val decorator = WithTimeoutDecorator(timeoutMillis)

            val mockFunction: suspend () -> String = {
                delay(1500)
                "result"
            }

            shouldThrowExactly<TimeoutCancellationException> {
                runBlocking {
                    decorator.decorate("test", mockFunction)
                }
            }
        }
    })
