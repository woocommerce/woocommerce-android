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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginScannerViewModelTest : BaseUnitTest() {

    private fun createViewModel(parser: QrLoginPayloadParser) = QrLoginScannerViewModel(
        savedState = SavedStateHandle(),
        parser = parser
    )

    private fun success(raw: String) = CodeScannerStatus.Success(code = raw, format = BarcodeFormat.FormatQRCode)

    @Test
    fun `given SiteAppPassword payload, when scan result received, then emits SiteAppPassword dispatch`() {
        val payload = QrLoginPayload.SiteAppPassword(
            siteUrl = "https://store.example",
            username = "admin",
            appPassword = "abc"
        )
        val parser: QrLoginPayloadParser = mock {
            on { parse("raw") } doReturn payload
        }
        val viewModel = createViewModel(parser)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(success("raw"))

        assertThat(events.last()).isEqualTo(QrLoginScannerViewModel.Dispatch.SiteAppPassword(payload))
    }

    @Test
    fun `given WpComToken payload, when scan result received, then emits WpComToken dispatch`() {
        val payload = QrLoginPayload.WpComToken(token = "t")
        val parser: QrLoginPayloadParser = mock {
            on { parse("raw") } doReturn payload
        }
        val viewModel = createViewModel(parser)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(success("raw"))

        assertThat(events.last()).isEqualTo(QrLoginScannerViewModel.Dispatch.WpComToken(payload))
    }

    @Test
    fun `given UrlOnly payload, when scan result received, then emits UrlOnly dispatch`() {
        val payload = QrLoginPayload.UrlOnly(siteUrl = "https://store.example")
        val parser: QrLoginPayloadParser = mock {
            on { parse("raw") } doReturn payload
        }
        val viewModel = createViewModel(parser)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(success("raw"))

        assertThat(events.last()).isEqualTo(QrLoginScannerViewModel.Dispatch.UrlOnly(payload))
    }

    @Test
    fun `given Invalid payload, when scan result received, then emits InvalidPayload dispatch`() {
        val parser: QrLoginPayloadParser = mock {
            on { parse("raw") } doReturn QrLoginPayload.Invalid
        }
        val viewModel = createViewModel(parser)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(success("raw"))

        assertThat(events.last()).isEqualTo(QrLoginScannerViewModel.Dispatch.InvalidPayload)
    }

    @Test
    fun `given scanner Failure, when scan result received, then emits ScannerFailure dispatch`() {
        val parser: QrLoginPayloadParser = mock()
        val viewModel = createViewModel(parser)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(
            CodeScannerStatus.Failure(error = "boom", type = CodeScanningErrorType.Unknown)
        )

        assertThat(events.last()).isEqualTo(QrLoginScannerViewModel.Dispatch.ScannerFailure)
    }

    @Test
    fun `given NotFound status, when scan result received, then no dispatch emitted`() {
        val parser: QrLoginPayloadParser = mock()
        val viewModel = createViewModel(parser)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(CodeScannerStatus.NotFound)

        assertThat(events).isEmpty()
    }

    @Test
    fun `given successful dispatch, when another scan arrives, then ignored to avoid double-submit`() {
        val payload = QrLoginPayload.UrlOnly(siteUrl = "https://store.example")
        val parser: QrLoginPayloadParser = mock {
            on { parse("raw") } doReturn payload
        }
        val viewModel = createViewModel(parser)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(success("raw"))
        viewModel.onScanResult(success("raw"))

        assertThat(events).hasSize(1)
    }

    @Test
    fun `given invalid payload, when another scan arrives with valid payload, then valid dispatch is emitted`() {
        val validPayload = QrLoginPayload.UrlOnly(siteUrl = "https://store.example")
        val parser: QrLoginPayloadParser = mock {
            on { parse("invalid") } doReturn QrLoginPayload.Invalid
            on { parse("valid") } doReturn validPayload
        }
        val viewModel = createViewModel(parser)
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(success("invalid"))
        viewModel.onScanResult(success("valid"))

        assertThat(events).hasSize(2)
        assertThat(events.last()).isEqualTo(QrLoginScannerViewModel.Dispatch.UrlOnly(validPayload))
    }
}
