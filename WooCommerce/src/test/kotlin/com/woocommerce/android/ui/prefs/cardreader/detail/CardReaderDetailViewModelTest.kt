package com.woocommerce.android.ui.prefs.cardreader.detail

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.CardReaderDetailEvent.CardReaderConnected
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.CardReaderDetailEvent.CardReaderDisconnected
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.CardReaderDetailEvent.CopyReadersNameToClipboard
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.ConnectedState
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.Loading
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.NotConnectedState
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private const val DUMMY_FIRMWARE_VERSION = "1.0.0.123-abcd-test-3000"

@ExperimentalCoroutinesApi
class CardReaderDetailViewModelTest : BaseUnitTest() {
    private val cardReaderManager: CardReaderManager = mock {
        onBlocking { readerStatus }.thenReturn(MutableStateFlow(CardReaderStatus.Connecting))
    }

    private val tracker: AnalyticsTrackerWrapper = mock()
    private val appPrefs: AppPrefs = mock()

    @Test
    fun `when view model init with connected state should emit loading view state`() {
        // GIVEN
        val status = MutableStateFlow(CardReaderStatus.Connected(mock()))
        whenever(cardReaderManager.readerStatus).thenReturn(status)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.viewStateData.value).isInstanceOf(Loading::class.java)
    }

    @Test
    fun `when view model init with connected state and update up to date should emit connected view state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            initConnectedState()

            // WHEN
            val viewModel = createViewModel()

            // THEN
            assertThat(viewModel.viewStateData.value).isInstanceOf(ConnectedState::class.java)
        }

    @Test
    fun `when view model init with connected state should emit correct values of connected state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            initConnectedState()

            // WHEN
            val viewModel = createViewModel()

            // THEN
            verifyConnectedState(
                viewModel,
                UiStringText(READER_NAME),
                UiStringRes(R.string.card_reader_detail_connected_battery_percentage, listOf(UiStringText("65"))),
                UiStringRes(
                    R.string.card_reader_detail_connected_firmware_version,
                    listOf(UiStringText(DUMMY_FIRMWARE_VERSION))
                ),
                updateAvailable = false
            )
        }

    @Test
    fun `when view model init with connected state and battery should emit correct values of connected state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            initConnectedState(batteryLevel = 0.325f)

            // WHEN
            val viewModel = createViewModel()

            // THEN
            verifyConnectedState(
                viewModel,
                UiStringText(READER_NAME),
                UiStringRes(R.string.card_reader_detail_connected_battery_percentage, listOf(UiStringText("33"))),
                UiStringRes(
                    R.string.card_reader_detail_connected_firmware_version,
                    listOf(UiStringText(DUMMY_FIRMWARE_VERSION))
                ),
                updateAvailable = false
            )
        }

    @Test
    fun `when view model init with connected state and empty name should emit connected view state with fallbacks`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            initConnectedState(readersName = null, batteryLevel = null)

            // WHEN
            val viewModel = createViewModel()
            viewModel.viewStateData.observeForever {}

            // THEN
            verifyConnectedState(
                viewModel,
                UiStringRes(R.string.card_reader_detail_connected_reader_unknown),
                null,
                UiStringRes(
                    R.string.card_reader_detail_connected_firmware_version,
                    listOf(UiStringText(DUMMY_FIRMWARE_VERSION))
                ),
                updateAvailable = false
            )
        }

    @Test
    fun `when view model init with not connected state should emit not connected view state`() {
        // GIVEN
        val status = MutableStateFlow(CardReaderStatus.NotConnected)
        whenever(cardReaderManager.readerStatus).thenReturn(status)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.viewStateData.value).isInstanceOf(NotConnectedState::class.java)
    }

    @Test
    fun `when view model init with not connected state should emit correct values not connected state`() {
        // GIVEN
        val status = MutableStateFlow(CardReaderStatus.NotConnected)
        whenever(cardReaderManager.readerStatus).thenReturn(status)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        verifyNotConnectedState(viewModel)
    }

    @Test
    fun `when view model init with connecting state should emit not connected view state`() {
        // GIVEN
        val status = MutableStateFlow(CardReaderStatus.Connecting)
        whenever(cardReaderManager.readerStatus).thenReturn(status)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.viewStateData.value).isInstanceOf(NotConnectedState::class.java)
    }

    @Test
    fun `when view model init with connected state should invoke software update availability`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = MutableStateFlow(CardReaderStatus.Connected(mock()))
            whenever(cardReaderManager.readerStatus).thenReturn(status)

            // WHEN
            createViewModel()

            // THEN
            verify(cardReaderManager).softwareUpdateAvailability
        }

    @Test
    fun `when view model init with connected state and update available should emit connected state with update`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            initConnectedState(updateAvailable = SoftwareUpdateAvailability.Available)

            // WHEN
            val viewModel = createViewModel()

            // THEN
            verifyConnectedState(
                viewModel,
                UiStringText(READER_NAME),
                UiStringRes(R.string.card_reader_detail_connected_battery_percentage, listOf(UiStringText("65"))),
                UiStringRes(
                    R.string.card_reader_detail_connected_firmware_version,
                    listOf(UiStringText(DUMMY_FIRMWARE_VERSION))
                ),
                updateAvailable = true
            )
        }

    @Test
    fun `when on update result with success should send snackbar event with success text`() {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUpdateReaderResult(UpdateResult.SUCCESS)

        // THEN
        assertThat(viewModel.event.value)
            .isEqualTo(Event.ShowSnackbar(R.string.card_reader_detail_connected_update_success))
    }

    @Test
    fun `given up to date software, when on update result successful, then connected state without update emitted`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()
            initConnectedState()

            // WHEN
            viewModel.onUpdateReaderResult(UpdateResult.SUCCESS)

            // THEN
            verifyConnectedState(
                viewModel,
                UiStringText(READER_NAME),
                UiStringRes(R.string.card_reader_detail_connected_battery_percentage, listOf(UiStringText("65"))),
                UiStringRes(
                    R.string.card_reader_detail_connected_firmware_version,
                    listOf(UiStringText(DUMMY_FIRMWARE_VERSION))
                ),
                updateAvailable = false
            )
        }
    }

    @Test
    fun `given connected state, when click on reader name, then copy and snackbar event triggers`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            initConnectedState()
            val viewModel = createViewModel()

            // WHEN
            val events = mutableListOf<Event>()
            viewModel.event.observeForever { events.add(it) }
            (viewModel.viewStateData.value as ConnectedState).onReaderNameLongClick.invoke()

            // THEN
            assertThat(events[1]).isEqualTo(CopyReadersNameToClipboard(READER_NAME))
            assertThat(events[2]).isEqualTo(
                Event.ShowSnackbar(
                    R.string.card_reader_detail_connected_readers_name_clipboard
                )
            )
        }
    }

    @Test
    fun `given software update available, when on update result successful, then connected without update emitted`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()
            initConnectedState(updateAvailable = SoftwareUpdateAvailability.Available)

            // WHEN
            viewModel.onUpdateReaderResult(UpdateResult.SUCCESS)

            // THEN
            verifyConnectedState(
                viewModel,
                UiStringText(READER_NAME),
                UiStringRes(R.string.card_reader_detail_connected_battery_percentage, listOf(UiStringText("65"))),
                UiStringRes(
                    R.string.card_reader_detail_connected_firmware_version,
                    listOf(UiStringText(DUMMY_FIRMWARE_VERSION))
                ),
                updateAvailable = false
            )
        }
    }

    @Test
    fun `when on update result with failed should send snackbar event with failed text`() {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUpdateReaderResult(UpdateResult.FAILED)

        // THEN
        assertThat(viewModel.event.value)
            .isEqualTo(Event.ShowSnackbar(R.string.card_reader_detail_connected_update_failed))
    }

    @Test
    fun `when on disconnect button clicked with success should do nothing`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            initConnectedState()
            whenever(cardReaderManager.disconnectReader()).thenReturn(true)
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ConnectedState).primaryButtonState!!.onActionClicked()

            // THEN
            assertThat(viewModel.viewStateData.value).isInstanceOf(ConnectedState::class.java)
        }

    @Test
    fun `when on disconnect button clicked with fail should change to not connected state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            initConnectedState()
            whenever(cardReaderManager.disconnectReader()).thenReturn(false)
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ConnectedState).primaryButtonState!!.onActionClicked()

            // THEN
            assertThat(viewModel.viewStateData.value).isInstanceOf(NotConnectedState::class.java)
        }

    @Test
    fun `when connect button clicked should track event`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = MutableStateFlow(CardReaderStatus.NotConnected)
            whenever(cardReaderManager.readerStatus).thenReturn(status)
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as NotConnectedState).onPrimaryActionClicked.invoke()

            // THEN
            verify(tracker).track(AnalyticsTracker.Stat.CARD_READER_DISCOVERY_TAPPED)
        }

    @Test
    fun `when disconnect button clicked should track event`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            initConnectedState()
            whenever(cardReaderManager.disconnectReader()).thenReturn(true)
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ConnectedState).primaryButtonState!!.onActionClicked()

            // THEN
            verify(tracker).track(AnalyticsTracker.Stat.CARD_READER_DISCONNECT_TAPPED)
        }

    @Test
    fun `when card reader disconnected successfully, then trigger accessibility announcement`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = MutableStateFlow(CardReaderStatus.NotConnected)
            whenever(cardReaderManager.readerStatus).thenReturn(status)

            // WHEN
            val viewModel = createViewModel()

            // THEN
            assertThat(viewModel.event.value)
                .isEqualTo(
                    CardReaderDisconnected(R.string.card_reader_accessibility_reader_is_disconnected)
                )
        }

    @Test
    fun `when card reader disconnection fails, then do not trigger accessibility announcement`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = MutableStateFlow(CardReaderStatus.Connected(mock()))
            whenever(cardReaderManager.readerStatus).thenReturn(status)

            // WHEN
            val viewModel = createViewModel()

            // THEN
            assertThat(viewModel.event.value)
                .isNotEqualTo(
                    CardReaderDisconnected(R.string.card_reader_accessibility_reader_is_disconnected)
                )
        }

    @Test
    fun `when card reader connected successfully, then trigger accessibility announcement`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = MutableStateFlow(CardReaderStatus.Connected(mock()))
            whenever(cardReaderManager.readerStatus).thenReturn(status)

            // WHEN
            val viewModel = createViewModel()

            // THEN
            assertThat(viewModel.event.value)
                .isEqualTo(
                    CardReaderConnected(R.string.card_reader_accessibility_reader_is_connected)
                )
        }

    @Test
    fun `when card reader connection fails, then do not trigger accessibility announcement`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = MutableStateFlow(CardReaderStatus.Connecting)
            whenever(cardReaderManager.readerStatus).thenReturn(status)

            // WHEN
            val viewModel = createViewModel()

            // THEN
            assertThat(viewModel.event.value)
                .isNotEqualTo(
                    CardReaderConnected(R.string.card_reader_accessibility_reader_is_connected)
                )
        }

    private fun verifyNotConnectedState(viewModel: CardReaderDetailViewModel) {
        val state = viewModel.viewStateData.value as NotConnectedState
        assertThat(state.headerLabel)
            .isEqualTo(UiStringRes(R.string.card_reader_detail_not_connected_header))
        assertThat(state.illustration)
            .isEqualTo(R.drawable.img_card_reader_not_connected)
        assertThat(state.firstHintLabel)
            .isEqualTo(UiStringRes(R.string.card_reader_detail_not_connected_first_hint_label))
        assertThat(state.secondHintLabel)
            .isEqualTo(UiStringRes(R.string.card_reader_detail_not_connected_second_hint_label))
        assertThat(state.thirdHintLabel)
            .isEqualTo(UiStringRes(R.string.card_reader_detail_not_connected_third_hint_label))
        assertThat(state.firstHintNumber)
            .isEqualTo(UiStringText("1"))
        assertThat(state.secondHintNumber)
            .isEqualTo(UiStringText("2"))
        assertThat(state.thirdHintNumber)
            .isEqualTo(UiStringText("3"))
        assertThat(state.connectBtnLabel)
            .isEqualTo(UiStringRes(R.string.card_reader_details_not_connected_connect_button_label))
    }

    private fun verifyConnectedState(
        viewModel: CardReaderDetailViewModel,
        readerName: UiString,
        batteryLevel: UiString?,
        firmwareVersion: UiString,
        updateAvailable: Boolean
    ) {
        val state = viewModel.viewStateData.value as ConnectedState
        assertThat(state.readerName).isEqualTo(readerName)
        assertThat(state.readerBattery).isEqualTo(batteryLevel)
        assertThat(state.readerFirmwareVersion).isEqualTo(firmwareVersion)
        if (updateAvailable) {
            assertThat(state.enforceReaderUpdate)
                .isEqualTo(UiStringRes(R.string.card_reader_detail_connected_enforced_update_software))
            assertThat(state.primaryButtonState?.text)
                .isEqualTo(UiStringRes(R.string.card_reader_detail_connected_update_software))
            assertThat(state.secondaryButtonState?.text)
                .isEqualTo(UiStringRes(R.string.card_reader_detail_connected_disconnect_reader))
        } else {
            assertThat(state.enforceReaderUpdate).isNull()
            assertThat(state.primaryButtonState?.text)
                .isEqualTo(UiStringRes(R.string.card_reader_detail_connected_disconnect_reader))
            assertThat(state.secondaryButtonState?.text).isNull()
        }
    }

    private fun initConnectedState(
        readersName: String? = READER_NAME,
        batteryLevel: Float? = 0.65F,
        firmwareVersion: String = DUMMY_FIRMWARE_VERSION,
        updateAvailable: SoftwareUpdateAvailability = SoftwareUpdateAvailability.NotAvailable
    ) = coroutinesTestRule.testDispatcher.runBlockingTest {
        val reader: CardReader = mock {
            on { this.id }.thenReturn(readersName)
            on { this.currentBatteryLevel }.thenReturn(batteryLevel)
            on { this.firmwareVersion }.thenReturn(firmwareVersion)
        }
        val status = MutableStateFlow(CardReaderStatus.Connected(reader))
        whenever(cardReaderManager.readerStatus).thenReturn(status)
        whenever(cardReaderManager.softwareUpdateAvailability).thenReturn(MutableStateFlow(updateAvailable))
    }

    private fun createViewModel() = CardReaderDetailViewModel(
        cardReaderManager,
        tracker,
        appPrefs,
        SavedStateHandle(),
    )

    private companion object {
        private const val READER_NAME = "CH3231H"
    }
}
