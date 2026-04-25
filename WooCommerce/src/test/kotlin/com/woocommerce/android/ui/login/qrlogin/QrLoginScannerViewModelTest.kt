package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.CodeScanningErrorType
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginScannerViewModelTest : BaseUnitTest() {

    private val ticket = QrLoginPayload.Ticket(token = "tok", siteUrl = "https://store.example")

    private val parser: QrLoginPayloadParser = mock()
    private val authenticator: QrLoginAuthenticator = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private val viewModel by lazy {
        QrLoginScannerViewModel(
            savedState = SavedStateHandle(),
            parser = parser,
            authenticator = authenticator,
            analyticsTracker = analyticsTracker
        )
    }

    @Before
    fun setUp() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(ticket)
        whenever(authenticator.authenticate(ticket)).thenReturn(Result.success(DEFAULT_SITE_ID))
    }

    @Test
    fun `given valid ticket and successful auth, when scan confirmed, then emits LoggedIn`() = testBlocking {
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()

        assertThat(events.last())
            .isEqualTo(QrLoginScannerViewModel.Dispatch.LoggedIn(localSiteId = DEFAULT_SITE_ID))
    }

    @Test
    fun `given valid ticket, when scan succeeds, then pending confirmation exposes host and no exchange`() =
        testBlocking {
            viewModel.onScanResult(successScan())

            assertThat(viewModel.pendingConfirmation.value).isEqualTo(
                QrLoginScannerViewModel.PendingConfirmation(ticket = ticket, host = "store.example")
            )
            verify(authenticator, never()).authenticate(any())
        }

    @Test
    fun `given pending confirmation, when cancel, then pending is cleared and authenticator not called`() =
        testBlocking {
            viewModel.onScanResult(successScan())
            viewModel.onCancelSite()

            assertThat(viewModel.pendingConfirmation.value).isNull()
            verify(authenticator, never()).authenticate(any())
        }

    @Test
    fun `given pending confirmation, when another scan arrives, then it is ignored`() = testBlocking {
        viewModel.onScanResult(successScan(RAW_SCAN))
        viewModel.onScanResult(successScan("second-raw"))

        assertThat(viewModel.pendingConfirmation.value?.ticket).isEqualTo(ticket)
        verify(parser, never()).parse("second-raw")
    }

    @Test
    fun `given canceled confirmation, when next valid scan, then a fresh confirmation is shown`() = testBlocking {
        viewModel.onScanResult(successScan())
        viewModel.onCancelSite()
        viewModel.onScanResult(successScan())

        assertThat(viewModel.pendingConfirmation.value?.ticket).isEqualTo(ticket)
    }

    @Test
    fun `given Invalid payload, when scan succeeds, then emits InvalidPayload error`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.Invalid)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.InvalidPayload)
        )
    }

    @Test
    fun `given Invalid payload, when scan succeeds, then authenticator is not called`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.Invalid)

        viewModel.onScanResult(successScan())

        verify(authenticator, never()).authenticate(any())
    }

    @Test
    fun `given scanner Failure, when scan result received, then emits Scanner error`() = testBlocking {
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(
            CodeScannerStatus.Failure(error = "boom", type = CodeScanningErrorType.Unknown)
        )

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.Scanner)
        )
    }

    @Test
    fun `given NotFound status, when scan result received, then no events emitted`() = testBlocking {
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(CodeScannerStatus.NotFound)

        assertThat(events).isEmpty()
    }

    @Test
    fun `given token rejected by server, when scan confirmed, then emits TokenRejected error`() = testBlocking {
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.TokenRejected))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.TokenRejected)
        )
    }

    @Test
    fun `given malformed exchange response, when scan confirmed, then emits ServerError`() = testBlocking {
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.MalformedResponse))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.ServerError)
        )
    }

    @Test
    fun `given server HttpError, when scan confirmed, then emits ServerError`() = testBlocking {
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.HttpError(500)))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.ServerError)
        )
    }

    @Test
    fun `given OnChangedException with UNAUTHORIZED site error, when scan confirmed, then emits SiteAuthFailure`() =
        testBlocking {
            whenever(authenticator.authenticate(ticket))
                .thenReturn(Result.failure(siteError(SiteErrorType.UNAUTHORIZED)))
            val events = viewModel.event.captureValues()

            viewModel.onScanResult(successScan())
            viewModel.onConfirmSite()

            assertThat(events.last()).isEqualTo(
                QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.SiteAuthFailure)
            )
        }

    @Test
    fun `given OnChangedException with WPCOM connectivity error, when scan confirmed, then emits Network`() =
        testBlocking {
            whenever(authenticator.authenticate(ticket))
                .thenReturn(Result.failure(siteError(SiteErrorType.WORDPRESS_COM_CONNECTIVITY_ERROR)))
            val events = viewModel.event.captureValues()

            viewModel.onScanResult(successScan())
            viewModel.onConfirmSite()

            assertThat(events.last()).isEqualTo(
                QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.Network)
            )
        }

    @Test
    fun `given OnChangedException with generic site error, when scan confirmed, then emits Unknown`() =
        testBlocking {
            whenever(authenticator.authenticate(ticket))
                .thenReturn(Result.failure(siteError(SiteErrorType.GENERIC_ERROR)))
            val events = viewModel.event.captureValues()

            viewModel.onScanResult(successScan())
            viewModel.onConfirmSite()

            assertThat(events.last()).isEqualTo(
                QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.Unknown)
            )
        }

    @Test
    fun `given network error, when scan confirmed, then emits Network error`() = testBlocking {
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.Network))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.Network)
        )
    }

    @Test
    fun `given rate limited error, when scan confirmed, then emits RateLimited error`() = testBlocking {
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.RateLimited))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.RateLimited)
        )
    }

    @Test
    fun `given endpoint missing, when scan confirmed, then endpointMissing flag is raised`() = testBlocking {
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.EndpointMissing))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()

        assertThat(events).isEmpty()
        assertThat(viewModel.endpointMissing.value).isTrue()
    }

    @Test
    fun `given endpoint missing flag raised, when retry called, then flag is cleared`() = testBlocking {
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.EndpointMissing))
        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()
        assertThat(viewModel.endpointMissing.value).isTrue()

        viewModel.onRetryAfterBlockingError()

        assertThat(viewModel.endpointMissing.value).isFalse()
    }

    @Test
    fun `given endpoint missing flag raised, when another scan arrives, then it is ignored`() = testBlocking {
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.EndpointMissing))
        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()

        viewModel.onScanResult(successScan())

        verify(authenticator, times(1)).authenticate(eq(ticket))
    }

    @Test
    fun `given site is not Woo, when auth fails with NotAWooSite, then emits NotAWooSite error`() = testBlocking {
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginAuthenticationException.NotAWooSite))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.NotAWooSite)
        )
    }

    @Test
    fun `given user is ineligible, when auth fails, then emits UserNotEligible error`() = testBlocking {
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginAuthenticationException.UserNotEligible(original = null)))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.UserNotEligible)
        )
    }

    @Test
    fun `given login succeeded, when another scan arrives, then ignored to avoid double-submit`() = testBlocking {
        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()
        viewModel.onScanResult(successScan())

        verify(authenticator, times(1)).authenticate(eq(ticket))
    }

    @Test
    fun `given exchange failure, when another valid scan arrives, then it is processed`() = testBlocking {
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.Network))
            .thenReturn(Result.success(99))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()
        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()

        assertThat(events.last()).isEqualTo(QrLoginScannerViewModel.Dispatch.LoggedIn(localSiteId = 99))
    }

    @Test
    fun `given Invalid payload, when scan invalid then valid, then second scan still processed`() = testBlocking {
        whenever(parser.parse("invalid")).thenReturn(QrLoginPayload.Invalid)
        whenever(parser.parse("valid")).thenReturn(ticket)
        whenever(authenticator.authenticate(ticket)).thenReturn(Result.success(11))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan("invalid"))
        viewModel.onScanResult(successScan("valid"))
        viewModel.onConfirmSite()

        assertThat(events.last()).isEqualTo(QrLoginScannerViewModel.Dispatch.LoggedIn(localSiteId = 11))
    }

    @Test
    fun `given successful login, when authenticator returns site id, then tracks LOGIN_QR_SUCCESS`() = testBlocking {
        viewModel.onScanResult(successScan())
        viewModel.onConfirmSite()

        verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_SUCCESS)
    }

    @Test
    fun `given scanner binding failure, when delivered as Failure, then tracks scan failed with scanner step`() =
        testBlocking {
            viewModel.onScanResult(
                CodeScannerStatus.Failure(error = "boom", type = CodeScanningErrorType.Unknown)
            )

            verify(analyticsTracker).track(
                stat = AnalyticsEvent.LOGIN_QR_SCAN_FAILED,
                properties = mapOf(AnalyticsTracker.KEY_STEP to "scanner"),
                errorContext = "CodeScanningErrorType\$Unknown",
                errorType = "Scanner",
                errorDescription = "boom"
            )
        }

    @Test
    fun `given exchange network error, when scan confirmed, then tracks scan failed with exchange step`() =
        testBlocking {
            whenever(authenticator.authenticate(ticket))
                .thenReturn(Result.failure(QrLoginExchangeException.Network))

            viewModel.onScanResult(successScan())
            viewModel.onConfirmSite()

            verify(analyticsTracker).track(
                stat = AnalyticsEvent.LOGIN_QR_SCAN_FAILED,
                properties = mapOf(AnalyticsTracker.KEY_STEP to "exchange"),
                errorContext = "QrLoginScannerViewModel",
                errorType = "Network",
                errorDescription = "Network failure during exchange"
            )
        }

    @Test
    fun `given invalid payload, when scan succeeds, then tracks scan failed with payload step`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.Invalid)

        viewModel.onScanResult(successScan())

        verify(analyticsTracker).track(
            stat = AnalyticsEvent.LOGIN_QR_SCAN_FAILED,
            properties = mapOf(AnalyticsTracker.KEY_STEP to "payload"),
            errorContext = null,
            errorType = "InvalidPayload",
            errorDescription = "Scanned QR did not match the expected deep link format"
        )
    }

    @Test
    fun `given deep link payload, when onDeepLinkPayload invoked, then exposes pending confirmation`() =
        testBlocking {
            viewModel.onDeepLinkPayload(RAW_SCAN)

            assertThat(viewModel.pendingConfirmation.value?.ticket).isEqualTo(ticket)
        }

    @Test
    fun `given deep link payload confirmed, when auth succeeds, then emits LoggedIn`() = testBlocking {
        val events = viewModel.event.captureValues()

        viewModel.onDeepLinkPayload(RAW_SCAN)
        viewModel.onConfirmSite()

        assertThat(events.last())
            .isEqualTo(QrLoginScannerViewModel.Dispatch.LoggedIn(localSiteId = DEFAULT_SITE_ID))
    }

    private fun successScan(raw: String = RAW_SCAN) =
        CodeScannerStatus.Success(code = raw, format = BarcodeFormat.FormatQRCode)

    private fun siteError(type: SiteErrorType): OnChangedException =
        OnChangedException(SiteError(type, "boom"))

    private companion object {
        const val RAW_SCAN = "raw"
        const val DEFAULT_SITE_ID = 42
    }
}
