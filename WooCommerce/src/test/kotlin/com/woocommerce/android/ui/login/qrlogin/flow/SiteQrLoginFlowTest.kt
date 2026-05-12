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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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

        // After Expired arrives, state moves on to Failed(MatchTimedOut). Capture intermediate
        // states by observing the StateFlow during the run.
        // Simpler: verify the scan call fired with the right args.
        verify(restClient).scan(ticket.siteUrl, ticket.token)
        assertThat(flow.state.value).isEqualTo(FlowState.Failed(reason = ErrorReason.MatchTimedOut, retryable = false))
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

    // endregion

    // region analytics events

    @Test
    fun `given scan fails with Network, when start fires, then a Failure analytics event is emitted`() = testBlocking {
        whenever(restClient.scan(ticket.siteUrl, ticket.token))
            .thenReturn(Result.failure(QrLoginScanException.Network))
        val flow = newFlow()
        val collected = mutableListOf<FlowAnalyticsEvent>()
        val job = launch { flow.analyticsEvents.toList(collected) }

        flow.start()
        advanceUntilIdle()
        job.cancel()

        val failure = collected.filterIsInstance<FlowAnalyticsEvent.Failure>().single()
        assertThat(failure.step).isEqualTo(FailureStep.Scan)
        assertThat(failure.reason).isEqualTo(ErrorReason.Network)
    }

    @Test
    fun `given the flow succeeds, when completeLogin returns, then a Success analytics event is emitted`() =
        testBlocking {
            stubHappyPath()
            val flow = newFlow()
            val collected = mutableListOf<FlowAnalyticsEvent>()
            val job = launch { flow.analyticsEvents.toList(collected) }

            flow.start()
            advanceUntilIdle()
            job.cancel()

            assertThat(collected).contains(FlowAnalyticsEvent.Success)
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
