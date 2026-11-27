package com.woocommerce.android.ui.woopos.settings.details.hardware.receiptprinter

import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosReceiptPrinterFacade
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@ExperimentalCoroutinesApi
class WooPosSettingsHardwareReceiptPrinterViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val connectionStatus = MutableStateFlow<ReceiptPrinterConnectionStatus>(
        ReceiptPrinterConnectionStatus.Disconnected
    )
    private val receiptPrinterFacade: WooPosReceiptPrinterFacade = mock {
        on { connectionStatus }.thenReturn(connectionStatus)
    }
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()

    @Test
    fun `given no printer connected, when created, then state is disconnected`() = runTest {
        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.uiState.value).isEqualTo(WooPosSettingsHardwareReceiptPrinterUiState.Disconnected)
    }

    @Test
    fun `given a connected printer, when created, then state is connected with the printer name`() = runTest {
        // GIVEN
        connectionStatus.value = ReceiptPrinterConnectionStatus.Connected(PRINTER)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.uiState.value)
            .isEqualTo(WooPosSettingsHardwareReceiptPrinterUiState.Connected(PRINTER.name))
    }

    @Test
    fun `when a printer connects, then state is connected with the printer name`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        connectionStatus.value = ReceiptPrinterConnectionStatus.Connected(PRINTER)
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.uiState.value)
            .isEqualTo(WooPosSettingsHardwareReceiptPrinterUiState.Connected(PRINTER.name))
    }

    @Test
    fun `when a printer is connecting, then state is disconnected`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        connectionStatus.value = ReceiptPrinterConnectionStatus.Connecting(PRINTER)
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.uiState.value).isEqualTo(WooPosSettingsHardwareReceiptPrinterUiState.Disconnected)
    }

    @Test
    fun `given a connected printer, when it is disconnecting, then state is disconnected`() = runTest {
        // GIVEN
        connectionStatus.value = ReceiptPrinterConnectionStatus.Connected(PRINTER)
        val viewModel = createViewModel()

        // WHEN
        connectionStatus.value = ReceiptPrinterConnectionStatus.Disconnecting
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.uiState.value).isEqualTo(WooPosSettingsHardwareReceiptPrinterUiState.Disconnected)
    }

    @Test
    fun `when connect clicked, then sends show receipt printer setup dialog event`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onConnectClicked()
        advanceUntilIdle()

        // THEN
        verify(childrenToParentEventSender).sendToParent(
            ChildToParentEvent.SettingsEvent.ShowReceiptPrinterSetupDialog
        )
    }

    @Test
    fun `when disconnect clicked, then disconnects the printer`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onDisconnectClicked()
        advanceUntilIdle()

        // THEN
        verify(receiptPrinterFacade).disconnect()
    }

    @Test
    fun `when disconnect clicked, then disconnect tapped is tracked`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onDisconnectClicked()
        advanceUntilIdle()

        // THEN
        verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.ReceiptPrinterDisconnectTapped)
    }

    @Test
    fun `when a connected printer disconnects by itself, then disconnect tapped is not tracked`() = runTest {
        // GIVEN
        connectionStatus.value = ReceiptPrinterConnectionStatus.Connected(PRINTER)
        createViewModel()

        // WHEN
        connectionStatus.value = ReceiptPrinterConnectionStatus.Disconnected
        advanceUntilIdle()

        // THEN
        verifyNoInteractions(analyticsTracker)
    }

    private fun TestScope.createViewModel() = WooPosSettingsHardwareReceiptPrinterViewModel(
        receiptPrinterFacade = receiptPrinterFacade,
        childrenToParentEventSender = childrenToParentEventSender,
        analyticsTracker = analyticsTracker,
    ).also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private companion object {
        val PRINTER = ReceiptPrinterDevice(id = "printer-id", name = "Star mC-Print3")
    }
}
