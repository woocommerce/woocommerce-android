package com.woocommerce.android.ui.woopos.roles.pin

import com.woocommerce.android.ui.woopos.roles.lock.WooPosLocalRateLimiter
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosClearLocalPINDataTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()
    private val store: WooPosLocalPINStore = mock()
    private val rateLimiter: WooPosLocalRateLimiter = mock()
    private val clearLocalPINData = WooPosClearLocalPINData(store, rateLimiter)

    @Test
    fun `given PIN cleanup fails, when clearing local data, then attempt cleanup still runs`() = runTest {
        // GIVEN
        whenever(store.clearAll()).thenReturn(Result.failure(WooPosPINError.StorageFailure))
        whenever(rateLimiter.clearAllAttempts()).thenReturn(Result.success(Unit))

        // WHEN
        clearLocalPINData()

        // THEN
        inOrder(store, rateLimiter) {
            verify(store).clearAll()
            verify(rateLimiter).clearAllAttempts()
        }
    }

    @Test
    fun `given attempt cleanup fails, when clearing local data, then PIN cleanup still completes`() = runTest {
        // GIVEN
        whenever(store.clearAll()).thenReturn(Result.success(Unit))
        whenever(rateLimiter.clearAllAttempts()).thenReturn(Result.failure(WooPosPINError.StorageFailure))

        // WHEN
        clearLocalPINData()

        // THEN
        verify(store).clearAll()
        verify(rateLimiter).clearAllAttempts()
    }

    @Test
    fun `given suspended PIN cleanup, when the caller cancels, then both stores finish cleanup`() = runTest {
        // GIVEN
        val gate = CompletableDeferred<Unit>()
        whenever(store.clearAll()).doSuspendableAnswer {
            gate.await()
            Result.success(Unit)
        }
        whenever(rateLimiter.clearAllAttempts()).thenReturn(Result.success(Unit))
        val cleanup = launch { clearLocalPINData() }
        runCurrent()

        // WHEN
        cleanup.cancel()
        gate.complete(Unit)
        cleanup.join()

        // THEN
        assertThat(cleanup.isCancelled).isTrue()
        verify(rateLimiter).clearAllAttempts()
    }
}
