package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class CardReaderRemoteCollectQueueTest : CardReaderBaseUnitTest() {
    private val logWrapper: LogWrapper = mock()

    @Test
    fun `given A runs and queued B is cancelled, when C is enqueued, then C waits for A`() = testBlocking {
        // GIVEN
        val queue = CardReaderRemoteCollectQueue(this, logWrapper)
        val started = mutableListOf<String>()
        queue.enqueue("A") {
            started += "A"
            awaitCancellation()
        }
        queue.enqueue("B") { started += "B" }
        queue.cancel("B")

        // WHEN
        queue.enqueue("C") { started += "C" }

        // THEN
        assertThat(started).containsExactly("A")
        queue.cancel("A")
        assertThat(started).containsExactly("A", "C")
    }

    @Test
    fun `given Stripe throws from its cancellation handler, when collect is cancelled, then failure is logged`() =
        testBlocking {
            // GIVEN
            val queue = CardReaderRemoteCollectQueue(this, logWrapper)
            queue.enqueue("A") {
                suspendCancellableCoroutine<Unit> { continuation ->
                    continuation.invokeOnCancellation { error("Cancellation race") }
                }
            }

            // WHEN
            queue.cancel("A")

            // THEN
            verify(logWrapper).e(any(), any())
        }

    @Test
    fun `given a queued request, when cancellation claims it, then it cannot become the reader owner`() = testBlocking {
        // GIVEN
        val queue = CardReaderRemoteCollectQueue(this, logWrapper)
        val started = mutableListOf<String>()
        queue.enqueue("A") {
            started += "A"
            awaitCancellation()
        }
        queue.enqueue("B") { started += "B" }

        // WHEN
        assertThat(queue.cancelQueued("B")).isTrue()
        queue.cancel("A")

        // THEN
        assertThat(started).containsExactly("A")
        assertThat(queue.isCurrent("B")).isFalse()
    }

    @Test
    fun `given a newer reader owner, when an old cancel arrives, then the old request is not current`() = testBlocking {
        // GIVEN
        val queue = CardReaderRemoteCollectQueue(this, logWrapper)
        queue.enqueue("A") { }
        assertThat(queue.isCurrent("A")).isTrue()
        queue.enqueue("B") { }

        // THEN
        assertThat(queue.isCurrent("A")).isFalse()
        assertThat(queue.cancelQueued("A")).isFalse()
        assertThat(queue.isCurrent("B")).isTrue()
    }
}
