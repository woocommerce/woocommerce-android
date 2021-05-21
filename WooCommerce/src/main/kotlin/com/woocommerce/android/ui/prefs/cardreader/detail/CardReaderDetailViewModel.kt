package com.woocommerce.android.ui.prefs.cardreader.detail

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReader
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.CardReaderStatus.Connected
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderConnectScreen
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.ConnectedState
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.ConnectedState.ButtonState
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.NotConnectedState
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class CardReaderDetailViewModel @Inject constructor(
    // TODO cardreader change this to non-nullable
    val cardReaderManager: CardReaderManager?,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val viewState = MutableLiveData<ViewState>()
    val viewStateData: LiveData<ViewState> = viewState

    init {
        launch {
            cardReaderManager!!.readerStatus.collect { status ->
                when (status) {
                    is Connected -> viewState.value = ConnectedState(
                        enforceReaderUpdate = UiStringRes(
                            R.string.card_reader_detail_connected_enforced_update_software
                        ),
                        readerName = status.cardReader.getReadersName(),
                        readerBattery = status.cardReader.getReadersBatteryLevel(),
                        primaryButtonState = null,
                        secondaryButtonState = ButtonState(
                            onActionClicked = ::onDisconnectClicked,
                            text = UiStringRes(R.string.card_reader_detail_connected_disconnect_reader)
                        )
                    )
                    else -> viewState.value = NotConnectedState(
                        onPrimaryActionClicked = ::onConnectBtnClicked
                    )
                }.exhaustive
            }
        }
    }

    private fun onConnectBtnClicked() {
        triggerEvent(CardReaderConnectScreen)
    }

    private fun onUpdateReaderClicked() {
        // TODO cardreader implement update functionality
    }

    private fun onDisconnectClicked() {
        // TODO cardreader implement disconnect functionality
    }

    private fun CardReader.getReadersName(): UiString {
        return with(id) {
            if (isNullOrEmpty())
                UiStringRes(R.string.card_reader_detail_connected_reader_unknown)
            else UiStringText(this)
        }
    }

    private fun CardReader.getReadersBatteryLevel(): UiString? {
        return currentBatteryLevel?.let {
            UiStringRes(
                R.string.card_reader_detail_connected_battery_percentage,
                listOf(UiStringText(it.roundToInt().toString()))
            )
        }
    }

    sealed class NavigationTarget : Event() {
        object CardReaderConnectScreen : NavigationTarget()
    }

    sealed class ViewState {
        data class NotConnectedState(
            val onPrimaryActionClicked: (() -> Unit)
        ) : ViewState() {
            val headerLabel = UiStringRes(R.string.card_reader_detail_not_connected_header)
            @DrawableRes val illustration = R.drawable.img_card_reader_not_connected
            val firstHintLabel = UiStringRes(R.string.card_reader_detail_not_connected_first_hint_label)
            val secondHintLabel = UiStringRes(R.string.card_reader_detail_not_connected_second_hint_label)
            val connectBtnLabel = UiStringRes(R.string.card_reader_details_not_connected_connect_button_label)
        }

        data class ConnectedState(
            val enforceReaderUpdate: UiString,
            val readerName: UiString,
            val readerBattery: UiString?,
            val primaryButtonState: ButtonState?,
            val secondaryButtonState: ButtonState?
        ) : ViewState() {
            data class ButtonState(
                val onActionClicked: (() -> Unit),
                val text: UiString
            )
        }
    }
}
