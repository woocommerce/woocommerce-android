package com.woocommerce.android.ui.prefs.cardreader.detail

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReader
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.CardReaderStatus
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.ConnectedState
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.NotConnectedState
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CardReaderDetailViewModelTest : BaseUnitTest() {
    private val cardReaderManager: CardReaderManager = mock()

    @Test
    fun `when view model init with connected state should emit connected view state`() {
        // GIVEN
        val batteryLevel = 1.6F
        val readerName = "CH3231H"
        val reader: CardReader = mock {
            on { id }.thenReturn(readerName)
            on { currentBatteryLevel }.thenReturn(batteryLevel)
        }
        val status = MutableStateFlow(CardReaderStatus.Connected(reader))
        whenever(cardReaderManager.readerStatus).thenReturn(status)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        verifyConnectedState(
            viewModel,
            UiStringText(readerName),
            UiStringRes(R.string.card_reader_detail_connected_battery_percentage, listOf(UiStringText("1.6")))
        )
    }

    @Test
    fun `when view model init with connected state and empty name should emit connected view state with fallbacks`() {
        // GIVEN
        val reader: CardReader = mock {
            on { id }.thenReturn(null)
            on { currentBatteryLevel }.thenReturn(null)
        }
        val status = MutableStateFlow(CardReaderStatus.Connected(reader))
        whenever(cardReaderManager.readerStatus).thenReturn(status)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        verifyConnectedState(
            viewModel,
            UiStringRes(R.string.card_reader_detail_connected_reader_unknown),
            null
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
        verifyNotConnectedState(viewModel)
    }

    @Test
    fun `when view model init with connection state should emit not connected view state`() {
        // GIVEN
        val status = MutableStateFlow(CardReaderStatus.Connecting)
        whenever(cardReaderManager.readerStatus).thenReturn(status)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        verifyNotConnectedState(viewModel)
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
        assertThat(state.connectBtnLabel)
            .isEqualTo(UiStringRes(R.string.card_reader_details_not_connected_connect_button_label))
    }

    private fun verifyConnectedState(
        viewModel: CardReaderDetailViewModel,
        readerName: UiString,
        batteryLevel: UiString?
    ) {
        val state = viewModel.viewStateData.value as ConnectedState
        assertThat(state.enforceReaderUpdate)
            .isEqualTo(UiStringRes(R.string.card_reader_detail_connected_enforced_update_software))
        assertThat(state.readerName).isEqualTo(readerName)
        assertThat(state.readerBattery).isEqualTo(batteryLevel)
        assertThat(state.primaryButtonState?.text).isNull()
        assertThat(state.secondaryButtonState?.text)
            .isEqualTo(UiStringRes(R.string.card_reader_detail_connected_disconnect_reader))
    }

    private fun createViewModel() = CardReaderDetailViewModel(
        cardReaderManager,
        SavedStateHandle()
    )
}
