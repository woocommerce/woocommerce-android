package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterError
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosPermissionUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class WooPosReceiptPrinterSetupViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val controllerState = MutableStateFlow<WooPosReceiptPrinterConnectionState>(
        WooPosReceiptPrinterConnectionState.Idle
    )
    private val controller: WooPosReceiptPrinterConnectionController = mock {
        on { state }.thenReturn(controllerState)
    }
    private val controllerFactory: WooPosReceiptPrinterConnectionController.Factory = mock {
        on { create(any()) }.thenReturn(controller)
    }
    private val permissionUtils: WooPosPermissionUtils = mock()

    @Test
    fun `when the controller state changes, then the state follows it`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        controllerState.value = WooPosReceiptPrinterConnectionState.FoundOne(PRINTER)

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosReceiptPrinterConnectionState.FoundOne(PRINTER))
    }

    @Test
    fun `given no setup is running, when setup starts, then the controller starts setup`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.startSetup()

        // THEN
        verify(controller).startSetup()
    }

    @Test
    fun `given a setup is running, when setup starts again, then the controller is not restarted`() = runTest {
        // GIVEN
        controllerState.value = WooPosReceiptPrinterConnectionState.Connecting(PRINTER)
        val viewModel = createViewModel()

        // WHEN
        viewModel.startSetup()

        // THEN
        verify(controller, never()).startSetup()
    }

    @Test
    fun `when the dialog is dismissed, then the controller cancels setup`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.dismissDialog()

        // THEN
        verify(controller).cancelSetup()
    }

    @Test
    fun `when resumed, then the controller rechecks the Bluetooth requirement`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onResume()

        // THEN
        verify(controller).recheckBluetoothRequirement()
    }

    @Test
    fun `when Bluetooth is enabled, then the controller rechecks the Bluetooth requirement`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onBluetoothEnabled()

        // THEN
        verify(controller).recheckBluetoothRequirement()
    }

    @Test
    fun `when the Bluetooth permission result arrives, then it is passed to the controller`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onBluetoothPermissionResult(granted = false, shouldShowRationale = true)

        // THEN
        verify(controller).onBluetoothPermissionResult(granted = false, shouldShowRationale = true)
    }

    @Test
    fun `when open app settings is clicked, then the app settings are shown`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onOpenAppSettingsClicked()

        // THEN
        verify(permissionUtils).showAppSettings()
    }

    @Test
    fun `when open Bluetooth settings is clicked, then the Bluetooth settings are shown`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onOpenBluetoothSettingsClicked()

        // THEN
        verify(permissionUtils).showBluetoothSettings()
    }

    @Test
    fun `when connect is clicked, then the controller connects to that printer`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onConnectClicked(PRINTER)

        // THEN
        verify(controller).connect(PRINTER)
    }

    @Test
    fun `when keep searching is clicked, then the controller keeps searching`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onKeepSearchingClicked()

        // THEN
        verify(controller).keepSearching()
    }

    @Test
    fun `given a failed setup, when search again is clicked, then the controller starts setup`() = runTest {
        // GIVEN
        controllerState.value = WooPosReceiptPrinterConnectionState.ConnectionFailed(
            device = PRINTER,
            error = ReceiptPrinterError.ConnectionLost(),
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onSearchAgainClicked()

        // THEN
        verify(controller).startSetup()
    }

    private fun createViewModel() = WooPosReceiptPrinterSetupViewModel(
        controllerFactory = controllerFactory,
        permissionUtils = permissionUtils,
    )

    private companion object {
        val PRINTER = ReceiptPrinterDevice(id = "printer-id", name = "Star mC-Print3")
    }
}
