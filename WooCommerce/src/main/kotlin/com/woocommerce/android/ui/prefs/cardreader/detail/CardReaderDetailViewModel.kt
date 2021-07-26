package com.woocommerce.android.ui.prefs.cardreader.detail

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReader
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.SoftwareUpdateAvailability.CheckForUpdatesFailed
import com.woocommerce.android.cardreader.SoftwareUpdateAvailability.Initializing
import com.woocommerce.android.cardreader.SoftwareUpdateAvailability.UpToDate
import com.woocommerce.android.cardreader.SoftwareUpdateAvailability.UpdateAvailable
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderConnectScreen
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderUpdateScreen
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.ConnectedState
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.ConnectedState.ButtonState
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.Loading
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.NotConnectedState
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult.FAILED
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult.SKIPPED
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult.SUCCESS
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class CardReaderDetailViewModel @Inject constructor(
    val cardReaderManager: CardReaderManager,
    private val tracker: AnalyticsTrackerWrapper,
    private val appPrefs: AppPrefs,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val viewState = MutableLiveData<ViewState>(Loading)
    val viewStateData: LiveData<ViewState> = viewState

    init {
        launch {
            cardReaderManager.readerStatus.collect { status ->
                when (status) {
                    is Connected -> cardReaderManager.softwareUpdateAvailability().collect(::handleSoftwareUpdateStatus)
                    else -> showNotConnectedState()
                }
            }.exhaustive
        }
    }

    fun onUpdateReaderResult(updateResult: UpdateResult) {
        when (updateResult) {
            SUCCESS -> {
                handleSoftwareUpdateStatus(UpToDate)
                triggerEvent(Event.ShowSnackbar(R.string.card_reader_detail_connected_update_success))
            }
            FAILED -> triggerEvent(Event.ShowSnackbar(R.string.card_reader_detail_connected_update_failed))
            SKIPPED -> {
            }
        }.exhaustive
    }

    private fun showNotConnectedState() {
        viewState.value = NotConnectedState(onPrimaryActionClicked = ::onConnectBtnClicked)
    }

    private fun showConnectedState(readerStatus: Connected, updateAvailable: Boolean = false) {
        viewState.value = if (updateAvailable) {
            triggerEvent(CardReaderUpdateScreen(startedByUser = false))
            ConnectedState(
                enforceReaderUpdate = UiStringRes(
                    R.string.card_reader_detail_connected_enforced_update_software
                ),
                readerName = readerStatus.cardReader.getReadersName(),
                readerBattery = readerStatus.cardReader.getReadersBatteryLevel(),
                primaryButtonState = ButtonState(
                    onActionClicked = ::onUpdateReaderClicked,
                    text = UiStringRes(R.string.card_reader_detail_connected_update_software)
                ),
                secondaryButtonState = ButtonState(
                    onActionClicked = ::onDisconnectClicked,
                    text = UiStringRes(R.string.card_reader_detail_connected_disconnect_reader)
                )
            )
        } else {
            ConnectedState(
                enforceReaderUpdate = null,
                readerName = readerStatus.cardReader.getReadersName(),
                readerBattery = readerStatus.cardReader.getReadersBatteryLevel(),
                primaryButtonState = ButtonState(
                    onActionClicked = ::onDisconnectClicked,
                    text = UiStringRes(R.string.card_reader_detail_connected_disconnect_reader)
                ),
                secondaryButtonState = null
            )
        }
    }

    private fun onConnectBtnClicked() {
        tracker.track(AnalyticsTracker.Stat.CARD_READER_DISCOVERY_TAPPED)
        triggerEvent(CardReaderConnectScreen)
    }

    private fun onUpdateReaderClicked() {
        triggerEvent(CardReaderUpdateScreen(startedByUser = true))
    }

    private fun onDisconnectClicked() {
        tracker.track(AnalyticsTracker.Stat.CARD_READER_DISCONNECT_TAPPED)
        launch {
            clearLastKnowReader()
            val disconnectionResult = cardReaderManager.disconnectReader()
            if (!disconnectionResult) {
                WooLog.e(WooLog.T.CARD_READER, "Disconnection from reader has failed")
                showNotConnectedState()
            }
        }
    }

    private fun handleSoftwareUpdateStatus(updateStatus: SoftwareUpdateAvailability) {
        val readerStatus = cardReaderManager.readerStatus.value
        if (readerStatus !is Connected) return
        when (updateStatus) {
            Initializing -> viewState.value = Loading
            UpToDate -> showConnectedState(readerStatus)
            is UpdateAvailable -> showConnectedState(readerStatus, updateAvailable = true)
            CheckForUpdatesFailed -> showConnectedState(readerStatus).also {
                triggerEvent(Event.ShowSnackbar(R.string.card_reader_detail_connected_update_check_failed))
            }
        }.exhaustive
    }

    private fun clearLastKnowReader() {
        appPrefs.removeLastConnectedCardReaderId()
    }

    private fun CardReader.getReadersName(): UiString {
        return with(id) {
            if (isNullOrEmpty()) {
                UiStringRes(R.string.card_reader_detail_connected_reader_unknown)
            } else {
                UiStringText(this)
            }
        }
    }

    private fun CardReader.getReadersBatteryLevel(): UiString? {
        return currentBatteryLevel?.let {
            UiStringRes(
                R.string.card_reader_detail_connected_battery_percentage,
                listOf(UiStringText((it * 100).roundToInt().toString()))
            )
        }
    }

    sealed class NavigationTarget : Event() {
        object CardReaderConnectScreen : NavigationTarget()
        data class CardReaderUpdateScreen(val startedByUser: Boolean) : NavigationTarget()
    }

    sealed class ViewState {
        data class NotConnectedState(
            val onPrimaryActionClicked: (() -> Unit)
        ) : ViewState() {
            val headerLabel = UiStringRes(R.string.card_reader_detail_not_connected_header)
            @DrawableRes val illustration = R.drawable.img_card_reader_not_connected
            val firstHintNumber = UiStringText("1")
            val secondHintNumber = UiStringText("2")
            val thirdHintNumber = UiStringText("3")
            val firstHintLabel = UiStringRes(R.string.card_reader_detail_not_connected_first_hint_label)
            val secondHintLabel = UiStringRes(R.string.card_reader_detail_not_connected_second_hint_label)
            val thirdHintLabel = UiStringRes(R.string.card_reader_detail_not_connected_third_hint_label)
            val connectBtnLabel = UiStringRes(R.string.card_reader_details_not_connected_connect_button_label)
            val learnMoreLabel = UiStringRes(R.string.card_reader_detail_learn_more, containsHtml = true)
        }

        data class ConnectedState(
            val enforceReaderUpdate: UiString?,
            val readerName: UiString,
            val readerBattery: UiString?,
            val primaryButtonState: ButtonState?,
            val secondaryButtonState: ButtonState?
        ) : ViewState() {
            val learnMoreLabel = UiStringRes(R.string.card_reader_detail_learn_more, containsHtml = true)

            data class ButtonState(
                val onActionClicked: (() -> Unit),
                val text: UiString
            )
        }

        object Loading : ViewState()
    }
}
