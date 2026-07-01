package com.woocommerce.android.ui.login.qrlogin.flow

import com.woocommerce.android.network.qrlogin.QrLoginCredentials
import com.woocommerce.android.network.qrlogin.QrLoginExchangeException
import com.woocommerce.android.network.qrlogin.QrLoginRestClient
import com.woocommerce.android.network.qrlogin.QrLoginScanException
import com.woocommerce.android.network.qrlogin.QrLoginScanResult
import com.woocommerce.android.network.qrlogin.QrLoginSessionStatus
import com.woocommerce.android.network.qrlogin.QrLoginSessionStatusException
import com.woocommerce.android.ui.login.qrlogin.QrLoginAuthenticationException
import com.woocommerce.android.ui.login.qrlogin.QrLoginAuthenticator
import com.woocommerce.android.ui.login.qrlogin.QrLoginPayload
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SiteQrLoginFlowTest : BaseUnitTest() {

    private val ticket = QrLoginPayload.Ticket(token = "tok", siteUrl = "https://store.example")
    private val scanResult = QrLoginScanResult(
        sessionId = "sess-1",
        realNumber = "042",
        expiresInSeconds = 90,
    )
    private val credentials = QrLoginCredentials(
        userLogin = "admin",
        applicationPassword = "ap-secret",
    )

    private val restClient: QrLoginRestClient = mock()
    private val authenticator: QrLoginAuthenticator = mock()

    private fun CoroutineScope.newFlow() = SiteQrLoginFlow(
        ticket = ticket,
        scope = this,
        restClient = restClient,
        authenticator = authenticator,
    )

    // region scan

    @Test
    fun `given scan succeeds, when start fires, then state transitions to WaitingForApproval`() = testBlocking {
        whenever(restClient.scan(ticket.siteUrl, ticket.token)).thenReturn(Result.success(scanResult))
        // Make polling terminate so runTest's idle drain doesn't loop forever.
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Scanned))
            .thenReturn(Result.success(QrLoginSessionStatus.Expired))
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        verify(restClient).scan(ticket.siteUrl, ticket.token)
        val state = flow.state.value as FlowState.Failed
        assertThat(state.reason).isEqualTo(ErrorReason.MatchTimedOut)
        assertThat(state.retryable).isFalse()
    }

    @Test
    fun `given scan fails with TokenRejected, when start fires, then state is Failed with TokenRejected`() =
        testBlocking {
            whenever(restClient.scan(ticket.siteUrl, ticket.token))
                .thenReturn(Result.failure(QrLoginScanException.TokenRejected))
            val flow = newFlow()

            flow.start()
            advanceUntilIdle()

            val state = flow.state.value as FlowState.Failed
            assertThat(state.reason).isEqualTo(ErrorReason.TokenRejected)
            assertThat(state.retryable).isFalse()
        }

    @Test
    fun `given scan fails with Network, when start fires, then state is Failed with retryable Network`() =
        testBlocking {
            whenever(restClient.scan(ticket.siteUrl, ticket.token))
                .thenReturn(Result.failure(QrLoginScanException.Network))
            val flow = newFlow()

            flow.start()
            advanceUntilIdle()

            val state = flow.state.value as FlowState.Failed
            assertThat(state.reason).isEqualTo(ErrorReason.Network)
            assertThat(state.retryable).isTrue()
        }

    @Test
    fun `given scan fails with 409 AlreadyScanned, when start fires, then state is MatchAlreadyScanned`() =
        testBlocking {
            whenever(restClient.scan(ticket.siteUrl, ticket.token))
                .thenReturn(Result.failure(QrLoginScanException.AlreadyScanned))
            val flow = newFlow()

            flow.start()
            advanceUntilIdle()

            assertThat((flow.state.value as FlowState.Failed).reason).isEqualTo(ErrorReason.MatchAlreadyScanned)
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

            verify(restClient).exchange(ticket.siteUrl, ticket.token, "grant-1")
            verify(authenticator).completeLogin(ticket, credentials)
            assertThat(flow.state.value)
                .isEqualTo(FlowState.Completed(FlowCompletion.LoggedIn(localSiteId = LOCAL_SITE_ID)))
        }

    @Test
    fun `given polling returns Rejected, when start fires, then state is MatchRejected`() = testBlocking {
        whenever(restClient.scan(ticket.siteUrl, ticket.token)).thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Rejected))
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        assertThat((flow.state.value as FlowState.Failed).reason).isEqualTo(ErrorReason.MatchRejected)
    }

    @Test
    fun `given polling returns Expired, when start fires, then state is MatchTimedOut`() = testBlocking {
        whenever(restClient.scan(ticket.siteUrl, ticket.token)).thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Expired))
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        assertThat((flow.state.value as FlowState.Failed).reason).isEqualTo(ErrorReason.MatchTimedOut)
    }

    @Test
    fun `given polling hits rate-limit, when start fires, then state is Failed with retryable RateLimited`() =
        testBlocking {
            whenever(restClient.scan(ticket.siteUrl, ticket.token)).thenReturn(Result.success(scanResult))
            whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
                .thenReturn(Result.failure(QrLoginSessionStatusException.RateLimited))
            val flow = newFlow()

            flow.start()
            advanceUntilIdle()

            val state = flow.state.value as FlowState.Failed
            assertThat(state.reason).isEqualTo(ErrorReason.RateLimited)
            assertThat(state.retryable).isTrue()
        }

    // endregion

    // region exchange

    @Test
    fun `given exchange returns InvalidExchangeGrant, when polling approves, then state is MatchInvalidGrant`() =
        testBlocking {
            whenever(restClient.scan(ticket.siteUrl, ticket.token)).thenReturn(Result.success(scanResult))
            whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
                .thenReturn(Result.success(QrLoginSessionStatus.Approved("grant-1")))
            whenever(restClient.exchange(ticket.siteUrl, ticket.token, "grant-1"))
                .thenReturn(Result.failure(QrLoginExchangeException.InvalidExchangeGrant))
            val flow = newFlow()

            flow.start()
            advanceUntilIdle()

            val state = flow.state.value as FlowState.Failed
            assertThat(state.reason).isEqualTo(ErrorReason.MatchInvalidGrant)
        }

    @Test
    fun `given exchange returns NotApproved, when polling approves, then state is MatchTimedOut`() = testBlocking {
        whenever(restClient.scan(ticket.siteUrl, ticket.token)).thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Approved("grant-1")))
        whenever(restClient.exchange(ticket.siteUrl, ticket.token, "grant-1"))
            .thenReturn(Result.failure(QrLoginExchangeException.NotApproved))
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        assertThat((flow.state.value as FlowState.Failed).reason).isEqualTo(ErrorReason.MatchTimedOut)
    }

    // endregion

    // region auth (completeLogin)

    @Test
    fun `given completeLogin fails with NotAWooSite, when reached, then state is NotAWooSite without retry`() =
        testBlocking {
            stubHappyPath(
                authResult = Result.failure(QrLoginAuthenticationException.NotAWooSite)
            )
            val flow = newFlow()

            flow.start()
            advanceUntilIdle()

            val state = flow.state.value as FlowState.Failed
            assertThat(state.reason).isEqualTo(ErrorReason.NotAWooSite)
            assertThat(state.retryable).isFalse()
        }

    @Test
    fun `given completeLogin fails with UserNotEligible, when reached, then state is UserNotEligible`() = testBlocking {
        stubHappyPath(
            authResult = Result.failure(QrLoginAuthenticationException.UserNotEligible(original = null))
        )
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        assertThat((flow.state.value as FlowState.Failed).reason).isEqualTo(ErrorReason.UserNotEligible)
    }

    // endregion

    // region cancel / retry

    @Test
    fun `given the flow is in flight, when cancel is called, then state resets to Initial`() = testBlocking {
        stubHappyPath()
        val flow = newFlow()
        flow.start()

        flow.cancel()
        advanceUntilIdle()

        assertThat(flow.state.value).isEqualTo(FlowState.Initial)
    }

    @Test
    fun `given retryable scan failure, when retry is called, then scan is invoked again`() = testBlocking {
        whenever(restClient.scan(ticket.siteUrl, ticket.token))
            .thenReturn(Result.failure(QrLoginScanException.Network))
            .thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Expired))
        val flow = newFlow()
        flow.start()
        advanceUntilIdle()
        assertThat(flow.state.value).isInstanceOf(FlowState.Failed::class.java)

        flow.retry()
        advanceUntilIdle()

        verify(restClient, org.mockito.kotlin.times(2)).scan(ticket.siteUrl, ticket.token)
    }

    @Test
    fun `given retryable exchange failure with retained grant, when retry is called, then exchange is invoked again`() =
        testBlocking {
            whenever(restClient.scan(ticket.siteUrl, ticket.token)).thenReturn(Result.success(scanResult))
            whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
                .thenReturn(Result.success(QrLoginSessionStatus.Approved("grant-1")))
            whenever(restClient.exchange(ticket.siteUrl, ticket.token, "grant-1"))
                .thenReturn(Result.failure(QrLoginExchangeException.Network))
                .thenReturn(Result.success(credentials))
            whenever(authenticator.completeLogin(ticket, credentials)).thenReturn(Result.success(LOCAL_SITE_ID))
            val flow = newFlow()
            flow.start()
            advanceUntilIdle()
            assertThat((flow.state.value as FlowState.Failed).retryable).isTrue()

            flow.retry()
            advanceUntilIdle()

            verify(restClient, org.mockito.kotlin.times(2)).exchange(ticket.siteUrl, ticket.token, "grant-1")
            assertThat(flow.state.value)
                .isEqualTo(FlowState.Completed(FlowCompletion.LoggedIn(localSiteId = LOCAL_SITE_ID)))
        }

    @Test
    fun `given retryable poll failure, when retry is called, then session status is polled again with same session id and scan is not re-invoked`() =
        testBlocking {
            whenever(restClient.scan(ticket.siteUrl, ticket.token)).thenReturn(Result.success(scanResult))
            // Trip the poll-step bail-out (4 consecutive Network errors), then on retry let polling
            // succeed with Approved so the flow can complete end-to-end.
            whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
                .thenReturn(Result.failure(QrLoginSessionStatusException.Network))
                .thenReturn(Result.failure(QrLoginSessionStatusException.Network))
                .thenReturn(Result.failure(QrLoginSessionStatusException.Network))
                .thenReturn(Result.failure(QrLoginSessionStatusException.Network))
                .thenReturn(Result.success(QrLoginSessionStatus.Approved("grant-1")))
            whenever(restClient.exchange(ticket.siteUrl, ticket.token, "grant-1"))
                .thenReturn(Result.success(credentials))
            whenever(authenticator.completeLogin(ticket, credentials)).thenReturn(Result.success(LOCAL_SITE_ID))
            val flow = newFlow()
            flow.start()
            advanceUntilIdle()
            val failed = flow.state.value as FlowState.Failed
            assertThat(failed.failedAt).isEqualTo(FailureStep.Poll)
            assertThat(failed.retryable).isTrue()

            flow.retry()
            advanceUntilIdle()

            verify(restClient, org.mockito.kotlin.times(1)).scan(ticket.siteUrl, ticket.token)
            verify(restClient, org.mockito.kotlin.atLeast(5))
                .checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token)
            assertThat(flow.state.value)
                .isEqualTo(FlowState.Completed(FlowCompletion.LoggedIn(localSiteId = LOCAL_SITE_ID)))
        }

    @Test
    fun `given retryable poll failure after the approval window elapsed, when retry is called, then state is MatchTimedOut and polling does not resume`() =
        testBlocking {
            // expiresInSeconds = 0 means the retained snapshot's window is already elapsed by retry
            // time — resuming would re-show the stale "Expires in 0s" number the browser has killed.
            whenever(restClient.scan(ticket.siteUrl, ticket.token))
                .thenReturn(Result.success(scanResult.copy(expiresInSeconds = 0)))
            whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
                .thenReturn(Result.failure(QrLoginSessionStatusException.Network))
            val flow = newFlow()
            flow.start()
            advanceUntilIdle()
            val failed = flow.state.value as FlowState.Failed
            assertThat(failed.failedAt).isEqualTo(FailureStep.Poll)
            assertThat(failed.retryable).isTrue()

            flow.retry()
            advanceUntilIdle()

            val timedOut = flow.state.value as FlowState.Failed
            assertThat(timedOut.reason).isEqualTo(ErrorReason.MatchTimedOut)
            assertThat(timedOut.retryable).isFalse()
            assertThat(timedOut.failedAt).isEqualTo(FailureStep.Approve)
            // The dead session is not re-polled — only the original 4 consecutive failures ran.
            verify(restClient, org.mockito.kotlin.times(4))
                .checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token)
        }

    // endregion

    // region failedAt diagnostic field

    @Test
    fun `given scan fails, when start fires, then Failed carries failedAt=Scan`() = testBlocking {
        whenever(restClient.scan(ticket.siteUrl, ticket.token))
            .thenReturn(Result.failure(QrLoginScanException.Network))
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        assertThat((flow.state.value as FlowState.Failed).failedAt).isEqualTo(FailureStep.Scan)
    }

    @Test
    fun `given poll returns Rejected, when start fires, then Failed carries failedAt=Approve`() = testBlocking {
        whenever(restClient.scan(ticket.siteUrl, ticket.token)).thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Rejected))
        val flow = newFlow()

        flow.start()
        advanceUntilIdle()

        assertThat((flow.state.value as FlowState.Failed).failedAt).isEqualTo(FailureStep.Approve)
    }

    // endregion

    private suspend fun stubHappyPath(
        authResult: Result<Int> = Result.success(LOCAL_SITE_ID),
    ) {
        whenever(restClient.scan(ticket.siteUrl, ticket.token)).thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Approved("grant-1")))
        whenever(restClient.exchange(ticket.siteUrl, ticket.token, "grant-1"))
            .thenReturn(Result.success(credentials))
        whenever(authenticator.completeLogin(any(), any())).thenReturn(authResult)
    }

    private companion object {
        const val LOCAL_SITE_ID = 42
    }
}
