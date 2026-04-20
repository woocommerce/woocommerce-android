package com.woocommerce.android.ui.woopos.settings.details.hardware.cardreader

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.BatteryStatus
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderConnectionController
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderConnectionControllerFactory
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosSettingsHardwareCardReaderViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val cardReaderFacade: WooPosCardReaderFacade = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val selectedSite: SelectedSite = mock()
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val controller: WooPosCardReaderConnectionController = mock()
    private val controllerFactory: WooPosCardReaderConnectionControllerFactory = mock {
        on { create(any()) }.thenReturn(controller)
    }

    private val readerStatusFlow = MutableStateFlow<CardReaderStatus>(
        CardReaderStatus.NotConnected()
    )
    private val softwareUpdateFlow = MutableStateFlow<SoftwareUpdateAvailability>(
        SoftwareUpdateAvailability.NotAvailable
    )
    private val batteryStatusFlow = MutableStateFlow<CardReaderBatteryStatus>(
        CardReaderBatteryStatus.Unknown
    )

    @Test
    fun `given disconnected reader, when init, then shows disconnected state`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(readerStatusFlow)
        whenever(cardReaderFacade.softwareUpdateAvailability).thenReturn(softwareUpdateFlow)
        whenever(cardReaderFacade.batteryStatus).thenReturn(batteryStatusFlow)
        readerStatusFlow.value = CardReaderStatus.NotConnected()

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.uiState.value).isInstanceOf(
            WooPosSettingsHardwareCardReaderUiState.Disconnected::class.java
        )
    }

    @Test
    fun `given connected reader, when init, then shows connected state`() = runTest {
        // GIVEN
        val mockReader = mock<CardReader> {
            whenever(it.id).thenReturn("Test Reader")
            whenever(it.currentBatteryLevel).thenReturn(0.75f)
            whenever(it.firmwareVersion).thenReturn("1.2.3")
        }
        whenever(cardReaderFacade.readerStatus).thenReturn(readerStatusFlow)
        whenever(cardReaderFacade.softwareUpdateAvailability).thenReturn(softwareUpdateFlow)
        whenever(cardReaderFacade.batteryStatus).thenReturn(batteryStatusFlow)
        // WHEN
        val viewModel = createViewModel()
        readerStatusFlow.value = CardReaderStatus.Connected(mockReader)
        advanceUntilIdle()

        // THEN
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(WooPosSettingsHardwareCardReaderUiState.Connected::class.java)
        val connectedState = uiState as WooPosSettingsHardwareCardReaderUiState.Connected
        assertThat(connectedState.readerName).isEqualTo("Test Reader")
        assertThat(connectedState.batteryLevel).isEqualTo(0.75f)
        assertThat(connectedState.firmwareVersion).isEqualTo("1.2.3")
    }

    @Test
    fun `when connect clicked, then sends show card reader connection dialog event`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(readerStatusFlow)
        whenever(cardReaderFacade.softwareUpdateAvailability).thenReturn(softwareUpdateFlow)
        whenever(cardReaderFacade.batteryStatus).thenReturn(batteryStatusFlow)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onConnectClicked()
        advanceUntilIdle()

        // THEN
        verify(childrenToParentEventSender).sendToParent(
            ChildToParentEvent.SettingsEvent.ShowCardReaderConnectionDialog
        )
    }

    @Test
    fun `when update clicked, then sends show card reader update dialog event`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(readerStatusFlow)
        whenever(cardReaderFacade.softwareUpdateAvailability).thenReturn(softwareUpdateFlow)
        whenever(cardReaderFacade.batteryStatus).thenReturn(batteryStatusFlow)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUpdateClick()
        advanceUntilIdle()

        // THEN
        verify(childrenToParentEventSender).sendToParent(
            ChildToParentEvent.SettingsEvent.ShowCardReaderUpdateDialog
        )
    }

    @Test
    fun `given connected reader, when software update available, then shows update available`() = runTest {
        // GIVEN
        val mockReader = mock<CardReader> {
            whenever(it.id).thenReturn("Test Reader")
            whenever(it.currentBatteryLevel).thenReturn(0.75f)
            whenever(it.firmwareVersion).thenReturn("1.2.3")
        }
        whenever(cardReaderFacade.readerStatus).thenReturn(readerStatusFlow)
        whenever(cardReaderFacade.softwareUpdateAvailability).thenReturn(softwareUpdateFlow)
        whenever(cardReaderFacade.batteryStatus).thenReturn(batteryStatusFlow)

        // WHEN
        val viewModel = createViewModel()
        readerStatusFlow.value = CardReaderStatus.Connected(mockReader)
        softwareUpdateFlow.value = SoftwareUpdateAvailability.Available
        advanceUntilIdle()

        // THEN
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(WooPosSettingsHardwareCardReaderUiState.Connected::class.java)
        val connectedState = uiState as WooPosSettingsHardwareCardReaderUiState.Connected
        assertThat(connectedState.isSoftwareUpdateAvailable).isTrue()
    }

    @Test
    fun `given connected reader, when software update not available, then shows update not available`() = runTest {
        // GIVEN
        val mockReader = mock<CardReader> {
            whenever(it.id).thenReturn("Test Reader")
            whenever(it.currentBatteryLevel).thenReturn(0.75f)
            whenever(it.firmwareVersion).thenReturn("1.2.3")
        }
        whenever(cardReaderFacade.readerStatus).thenReturn(readerStatusFlow)
        whenever(cardReaderFacade.softwareUpdateAvailability).thenReturn(softwareUpdateFlow)
        whenever(cardReaderFacade.batteryStatus).thenReturn(batteryStatusFlow)

        // WHEN
        val viewModel = createViewModel()
        readerStatusFlow.value = CardReaderStatus.Connected(mockReader)
        softwareUpdateFlow.value = SoftwareUpdateAvailability.NotAvailable
        advanceUntilIdle()

        // THEN
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(WooPosSettingsHardwareCardReaderUiState.Connected::class.java)
        val connectedState = uiState as WooPosSettingsHardwareCardReaderUiState.Connected
        assertThat(connectedState.isSoftwareUpdateAvailable).isFalse()
    }

    @Test
    fun `given connected reader, when battery status changes, then updates battery level`() = runTest {
        // GIVEN
        val mockReader = mock<CardReader> {
            whenever(it.id).thenReturn("Test Reader")
            whenever(it.currentBatteryLevel).thenReturn(0.75f)
            whenever(it.firmwareVersion).thenReturn("1.2.3")
        }
        whenever(cardReaderFacade.readerStatus).thenReturn(readerStatusFlow)
        whenever(cardReaderFacade.softwareUpdateAvailability).thenReturn(softwareUpdateFlow)
        whenever(cardReaderFacade.batteryStatus).thenReturn(batteryStatusFlow)

        val viewModel = createViewModel()
        readerStatusFlow.value = CardReaderStatus.Connected(mockReader)
        advanceUntilIdle()

        // WHEN
        batteryStatusFlow.value = CardReaderBatteryStatus.StatusChanged(
            batteryLevel = 0.15f,
            batteryStatus = BatteryStatus.LOW,
            isCharging = false
        )
        advanceUntilIdle()

        // THEN
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(WooPosSettingsHardwareCardReaderUiState.Connected::class.java)
        val connectedState = uiState as WooPosSettingsHardwareCardReaderUiState.Connected
        assertThat(connectedState.batteryLevel).isEqualTo(0.15f)
    }

    private fun createViewModel() = WooPosSettingsHardwareCardReaderViewModel(
        cardReaderFacade = cardReaderFacade,
        resourceProvider = resourceProvider,
        appPrefsWrapper = appPrefsWrapper,
        selectedSite = selectedSite,
        childrenToParentEventSender = childrenToParentEventSender,
        controllerFactory = controllerFactory
    )
}
