package com.woocommerce.android.ui.login.qrlogin.flow

import com.woocommerce.android.network.qrlogin.WpComQrLoginExchangeException
import com.woocommerce.android.network.qrlogin.WpComQrLoginExchangeResult
import com.woocommerce.android.network.qrlogin.WpComQrLoginRestClient
import com.woocommerce.android.network.qrlogin.WpComQrLoginScanException
import com.woocommerce.android.network.qrlogin.WpComQrLoginScanResult
import com.woocommerce.android.network.qrlogin.WpComQrLoginSessionStatus
import com.woocommerce.android.network.qrlogin.WpComQrLoginSessionStatusException
import com.woocommerce.android.ui.login.qrlogin.QrLoginPayload
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WpComQrLoginFlowTest : BaseUnitTest() {

    private val payload = QrLoginPayload.WpComToken(token = "tok", encrypted = "enc")
    private val scanResult = WpComQrLoginScanResult(
        sessionId = "wpc-sess-1",
        realNumber = "314",
        expiresInSeconds = 90,
        userEmail = "merchant@example.com",
    )

    private val restClient: WpComQrLoginRestClient = mock()

    private fun CoroutineScope.newFlow() = WpComQrLoginFlow(
        payload = payload,
        scope = this,
        restClient = restClient,
    )

    // region scan

    @Test
    fun `given scan succeeds, when start fires, then state moves through WaitingForApproval with email subtitle`() =
        testBlocking {
            whenever(restClient.scan(payload.token, payload.encrypted)).thenReturn(Result.success(scanResult))
            whenever(restClient.checkSessionStatus(scanResult.sessionId, payload.token))
                .thenReturn(Result.success(WpComQrLoginSessionStatus.Scanned))
                .thenReturn(Result.success(WpComQrLoginSessionStatus.Expired))
            val flow = newFlow()

            flow.start()
            advanceUntilIdle()

            verify(restClient).scan(payload.token, payload.encrypted)
            val state = flow.state.value as FlowState.Failed
            assertThat(state.reason).isEqualTo(ErrorReason.MatchTimedOut)
            assertThat(state.retryable).isFalse()
        }

    @Test
    fun `given scan fails with RestForbidden, when start fires, then state is TokenRejected`() = testBlocking {
        whenever(restClient.scan(payload.token, payload.encrypted))
            .thenReturn(Result.failure(WpComQrLoginScanException.RestForbidden))
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        val state = flow.state.value as FlowState.Failed
        assertThat(state.reason).isEqualTo(ErrorReason.TokenRejected)
        assertThat(state.retryable).isFalse()
    }

    @Test
    fun `given scan fails with AlreadyScanned, when start fires, then state is MatchAlreadyScanned`() = testBlocking {
        whenever(restClient.scan(payload.token, payload.encrypted))
            .thenReturn(Result.failure(WpComQrLoginScanException.AlreadyScanned))
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        assertThat((flow.state.value as FlowState.Failed).reason).isEqualTo(ErrorReason.MatchAlreadyScanned)
    }

    @Test
    fun `given scan fails with Network, when start fires, then state is Failed with retryable Network`() =
        testBlocking {
            whenever(restClient.scan(payload.token, payload.encrypted))
                .thenReturn(Result.failure(WpComQrLoginScanException.Network))
            val flow = newFlow()

            flow.start()
            advanceUntilIdle()

            val state = flow.state.value as FlowState.Failed
            assertThat(state.reason).isEqualTo(ErrorReason.Network)
            assertThat(state.retryable).isTrue()
        }

    // endregion

    // region polling

    @Test
    fun `given polling returns Approved, when start fires, then exchange is invoked and Completed is emitted`() =
        testBlocking {
            stubHappyPath()
            val flow = newFlow()

            flow.start()
            advanceUntilIdle()

            verify(restClient).exchange(payload.token, payload.encrypted, "wpc-grant-1")
            assertThat(flow.state.value)
                .isEqualTo(FlowState.Completed(FlowCompletion.OpenMagicLink(url = "https://wordpress.com/magic")))
        }

    @Test
    fun `given polling returns Rejected, when start fires, then state is MatchRejected`() = testBlocking {
        whenever(restClient.scan(payload.token, payload.encrypted)).thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(scanResult.sessionId, payload.token))
            .thenReturn(Result.success(WpComQrLoginSessionStatus.Rejected))
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        assertThat((flow.state.value as FlowState.Failed).reason).isEqualTo(ErrorReason.MatchRejected)
    }

    @Test
    fun `given polling returns Consumed, when start fires, then state is MatchAlreadyCompleted`() = testBlocking {
        whenever(restClient.scan(payload.token, payload.encrypted)).thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(scanResult.sessionId, payload.token))
            .thenReturn(Result.success(WpComQrLoginSessionStatus.Consumed))
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        assertThat((flow.state.value as FlowState.Failed).reason).isEqualTo(ErrorReason.MatchAlreadyCompleted)
    }

    @Test
    fun `given polling returns Expired, when start fires, then state is MatchTimedOut`() = testBlocking {
        whenever(restClient.scan(payload.token, payload.encrypted)).thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(scanResult.sessionId, payload.token))
            .thenReturn(Result.success(WpComQrLoginSessionStatus.Expired))
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        assertThat((flow.state.value as FlowState.Failed).reason).isEqualTo(ErrorReason.MatchTimedOut)
    }

    @Test
    fun `given polling hits rate-limit, when start fires, then state is RateLimited`() = testBlocking {
        whenever(restClient.scan(payload.token, payload.encrypted)).thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(scanResult.sessionId, payload.token))
            .thenReturn(Result.failure(WpComQrLoginSessionStatusException.RateLimited))
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        assertThat((flow.state.value as FlowState.Failed).reason).isEqualTo(ErrorReason.RateLimited)
    }

    @Test
    fun `given polling hits TokenHashMismatch, when start fires, then terminal TokenRejected`() = testBlocking {
        whenever(restClient.scan(payload.token, payload.encrypted)).thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(scanResult.sessionId, payload.token))
            .thenReturn(Result.failure(WpComQrLoginSessionStatusException.TokenHashMismatch))
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        val state = flow.state.value as FlowState.Failed
        assertThat(state.reason).isEqualTo(ErrorReason.TokenRejected)
        assertThat(state.retryable).isFalse()
    }

    // endregion

    // region exchange

    @Test
    fun `given exchange fails with AlreadyConsumed, when polling approves, then state is MatchAlreadyCompleted`() =
        testBlocking {
            whenever(restClient.scan(payload.token, payload.encrypted)).thenReturn(Result.success(scanResult))
            whenever(restClient.checkSessionStatus(scanResult.sessionId, payload.token))
                .thenReturn(Result.success(WpComQrLoginSessionStatus.Approved("wpc-grant-1")))
            whenever(restClient.exchange(payload.token, payload.encrypted, "wpc-grant-1"))
                .thenReturn(Result.failure(WpComQrLoginExchangeException.AlreadyConsumed))
            val flow = newFlow()

            flow.start()
            advanceUntilIdle()

            assertThat((flow.state.value as FlowState.Failed).reason).isEqualTo(ErrorReason.MatchAlreadyCompleted)
        }

    @Test
    fun `given exchange fails with InvalidExchangeGrant, when polling approves, then state is MatchInvalidGrant`() =
        testBlocking {
            whenever(restClient.scan(payload.token, payload.encrypted)).thenReturn(Result.success(scanResult))
            whenever(restClient.checkSessionStatus(scanResult.sessionId, payload.token))
                .thenReturn(Result.success(WpComQrLoginSessionStatus.Approved("wpc-grant-1")))
            whenever(restClient.exchange(payload.token, payload.encrypted, "wpc-grant-1"))
                .thenReturn(Result.failure(WpComQrLoginExchangeException.InvalidExchangeGrant))
            val flow = newFlow()

            flow.start()
            advanceUntilIdle()

            assertThat((flow.state.value as FlowState.Failed).reason).isEqualTo(ErrorReason.MatchInvalidGrant)
        }

    // endregion

    // region cancel / retry

    @Test
    fun `given flow is in flight, when cancel is called, then state resets to Initial`() = testBlocking {
        stubHappyPath()
        val flow = newFlow()
        flow.start()

        flow.cancel()
        advanceUntilIdle()

        assertThat(flow.state.value).isEqualTo(FlowState.Initial)
    }

    @Test
    fun `given retryable exchange failure, when retry is called, then exchange is invoked again`() = testBlocking {
        whenever(restClient.scan(payload.token, payload.encrypted)).thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(scanResult.sessionId, payload.token))
            .thenReturn(Result.success(WpComQrLoginSessionStatus.Approved("wpc-grant-1")))
        whenever(restClient.exchange(payload.token, payload.encrypted, "wpc-grant-1"))
            .thenReturn(Result.failure(WpComQrLoginExchangeException.Network))
            .thenReturn(Result.success(WpComQrLoginExchangeResult(magicLinkUrl = "https://wordpress.com/magic")))
        val flow = newFlow()
        flow.start()
        advanceUntilIdle()
        assertThat((flow.state.value as FlowState.Failed).retryable).isTrue()

        flow.retry()
        advanceUntilIdle()

        verify(restClient, times(2)).exchange(payload.token, payload.encrypted, "wpc-grant-1")
        assertThat(flow.state.value)
            .isEqualTo(FlowState.Completed(FlowCompletion.OpenMagicLink(url = "https://wordpress.com/magic")))
    }

    @Test
    fun `given retryable poll failure, when retry is called, then session status is polled again with same session id and scan is not re-invoked`() =
        testBlocking {
            whenever(restClient.scan(payload.token, payload.encrypted)).thenReturn(Result.success(scanResult))
            whenever(restClient.checkSessionStatus(scanResult.sessionId))
                .thenReturn(Result.failure(WpComQrLoginSessionStatusException.Network))
                .thenReturn(Result.failure(WpComQrLoginSessionStatusException.Network))
                .thenReturn(Result.failure(WpComQrLoginSessionStatusException.Network))
                .thenReturn(Result.failure(WpComQrLoginSessionStatusException.Network))
                .thenReturn(Result.success(WpComQrLoginSessionStatus.Approved("wpc-grant-1")))
            whenever(restClient.exchange(payload.token, payload.encrypted, "wpc-grant-1"))
                .thenReturn(Result.success(WpComQrLoginExchangeResult(magicLinkUrl = "https://wordpress.com/magic")))
            val flow = newFlow()
            flow.start()
            advanceUntilIdle()
            val failed = flow.state.value as FlowState.Failed
            assertThat(failed.failedAt).isEqualTo(FailureStep.Poll)
            assertThat(failed.retryable).isTrue()

            flow.retry()
            advanceUntilIdle()

            verify(restClient, times(1)).scan(payload.token, payload.encrypted)
            verify(restClient, atLeast(5)).checkSessionStatus(scanResult.sessionId)
            assertThat(flow.state.value)
                .isEqualTo(FlowState.Completed(FlowCompletion.OpenMagicLink(url = "https://wordpress.com/magic")))
        }

    // endregion

    private suspend fun stubHappyPath() {
        whenever(restClient.scan(payload.token, payload.encrypted)).thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(scanResult.sessionId, payload.token))
            .thenReturn(Result.success(WpComQrLoginSessionStatus.Approved("wpc-grant-1")))
        whenever(restClient.exchange(payload.token, payload.encrypted, "wpc-grant-1"))
            .thenReturn(Result.success(WpComQrLoginExchangeResult(magicLinkUrl = "https://wordpress.com/magic")))
    }
}
