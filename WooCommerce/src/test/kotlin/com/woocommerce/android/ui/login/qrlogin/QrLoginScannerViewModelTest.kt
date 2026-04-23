package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.CodeScanningErrorType
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginScannerViewModelTest : BaseUnitTest() {

    private val ticket = QrLoginPayload.Ticket(token = "tok", siteUrl = "https://store.example")

    private fun createViewModel(parser: QrLoginPayloadParser, authenticator: QrLoginAuthenticator) =
        QrLoginScannerViewModel(
            savedState = SavedStateHandle(),
            parser = parser,
            authenticator = authenticator
        )

    private fun successScan(raw: String) =
        CodeScannerStatus.Success(code = raw, format = BarcodeFormat.FormatQRCode)

    private fun parserReturning(payload: QrLoginPayload, forCode: String = "raw"): QrLoginPayloadParser =
        mock { on { parse(forCode) }.thenReturn(payload) }

    @Test
    fun `given valid ticket and successful auth, when scan succeeds, then emits LoggedIn`() = testBlocking {
        val authenticator: QrLoginAuthenticator = mock()
        whenever(authenticator.authenticate(ticket)).thenReturn(Result.success(42))
        val viewModel = createViewModel(parserReturning(ticket), authenticator)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan("raw"))

        assertThat(events.last()).isEqualTo(QrLoginScannerViewModel.Dispatch.LoggedIn(localSiteId = 42))
    }

    @Test
    fun `given Invalid payload, when scan succeeds, then emits InvalidPayload error`() = testBlocking {
        val viewModel = createViewModel(parserReturning(QrLoginPayload.Invalid), mock())
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan("raw"))

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.InvalidPayload)
        )
    }

    @Test
    fun `given scanner Failure, when scan result received, then emits Scanner error`() = testBlocking {
        val viewModel = createViewModel(mock(), mock())
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
        val viewModel = createViewModel(mock(), mock())
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(CodeScannerStatus.NotFound)

        assertThat(events).isEmpty()
    }

    @Test
    fun `given token rejected by server, when scan succeeds, then emits TokenRejected error`() = testBlocking {
        val authenticator: QrLoginAuthenticator = mock()
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.TokenRejected))
        val viewModel = createViewModel(parserReturning(ticket), authenticator)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan("raw"))

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.TokenRejected)
        )
    }

    @Test
    fun `given network error, when scan succeeds, then emits Network error`() = testBlocking {
        val authenticator: QrLoginAuthenticator = mock()
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.Network))
        val viewModel = createViewModel(parserReturning(ticket), authenticator)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan("raw"))

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.Network)
        )
    }

    @Test
    fun `given rate limited error, when scan succeeds, then emits RateLimited error`() = testBlocking {
        val authenticator: QrLoginAuthenticator = mock()
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.RateLimited))
        val viewModel = createViewModel(parserReturning(ticket), authenticator)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan("raw"))

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.RateLimited)
        )
    }

    @Test
    fun `given endpoint missing, when scan succeeds, then emits EndpointMissing error`() = testBlocking {
        val authenticator: QrLoginAuthenticator = mock()
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.EndpointMissing))
        val viewModel = createViewModel(parserReturning(ticket), authenticator)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan("raw"))

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.EndpointMissing)
        )
    }

    @Test
    fun `given site is not Woo, when auth fails with NotAWooSite, then emits NotAWooSite error`() = testBlocking {
        val authenticator: QrLoginAuthenticator = mock()
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginAuthenticationException.NotAWooSite))
        val viewModel = createViewModel(parserReturning(ticket), authenticator)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan("raw"))

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.NotAWooSite)
        )
    }

    @Test
    fun `given user is ineligible, when auth fails, then emits UserNotEligible error`() = testBlocking {
        val authenticator: QrLoginAuthenticator = mock()
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginAuthenticationException.UserNotEligible(original = null)))
        val viewModel = createViewModel(parserReturning(ticket), authenticator)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan("raw"))

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RecoverableError(QrLoginScannerViewModel.ErrorReason.UserNotEligible)
        )
    }

    @Test
    fun `given login succeeded, when another scan arrives, then ignored to avoid double-submit`() = testBlocking {
        val authenticator: QrLoginAuthenticator = mock()
        whenever(authenticator.authenticate(ticket)).thenReturn(Result.success(7))
        val viewModel = createViewModel(parserReturning(ticket), authenticator)

        viewModel.onScanResult(successScan("raw"))
        viewModel.onScanResult(successScan("raw"))

        verify(authenticator, times(1)).authenticate(eq(ticket))
    }

    @Test
    fun `given exchange failure, when another valid scan arrives, then it is processed`() = testBlocking {
        val authenticator: QrLoginAuthenticator = mock()
        whenever(authenticator.authenticate(ticket))
            .thenReturn(Result.failure(QrLoginExchangeException.Network))
            .thenReturn(Result.success(99))
        val viewModel = createViewModel(parserReturning(ticket), authenticator)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan("raw"))
        viewModel.onScanResult(successScan("raw"))

        assertThat(events.last()).isEqualTo(QrLoginScannerViewModel.Dispatch.LoggedIn(localSiteId = 99))
    }

    @Test
    fun `given Invalid payload, when scan invalid then valid, then second scan still processed`() = testBlocking {
        val parser: QrLoginPayloadParser = mock {
            on { parse("invalid") }.thenReturn(QrLoginPayload.Invalid)
            on { parse("valid") }.thenReturn(ticket)
        }
        val authenticator: QrLoginAuthenticator = mock()
        whenever(authenticator.authenticate(ticket)).thenReturn(Result.success(11))
        val viewModel = createViewModel(parser, authenticator)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan("invalid"))
        viewModel.onScanResult(successScan("valid"))

        assertThat(events.last()).isEqualTo(QrLoginScannerViewModel.Dispatch.LoggedIn(localSiteId = 11))
    }

    @Test
    fun `given Invalid payload, when scan succeeds, then authenticator is not called`() = testBlocking {
        val authenticator: QrLoginAuthenticator = mock()
        val viewModel = createViewModel(parserReturning(QrLoginPayload.Invalid), authenticator)

        viewModel.onScanResult(successScan("raw"))

        verify(authenticator, never()).authenticate(any())
    }
}
