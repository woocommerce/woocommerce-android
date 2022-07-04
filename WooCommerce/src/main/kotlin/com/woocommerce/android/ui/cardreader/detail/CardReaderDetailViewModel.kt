package com.woocommerce.android.ui.cardreader.detail

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connecting
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus.StatusChanged
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.CardReaderTracker
import com.woocommerce.android.ui.cardreader.detail.CardReaderDetailViewModel.CardReaderDetailEvent.NavigateToUrlInGenericWebView
import com.woocommerce.android.ui.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderConnectScreen
import com.woocommerce.android.ui.cardreader.detail.CardReaderDetailViewModel.ViewState.ConnectedState
import com.woocommerce.android.ui.cardreader.detail.CardReaderDetailViewModel.ViewState.ConnectedState.ButtonState
import com.woocommerce.android.ui.cardreader.detail.CardReaderDetailViewModel.ViewState.Loading
import com.woocommerce.android.ui.cardreader.detail.CardReaderDetailViewModel.ViewState.NotConnectedState
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.UpdateResult
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.UpdateResult.FAILED
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.UpdateResult.SUCCESS
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

private const val PERCENT_100 = 100

@HiltViewModel
class CardReaderDetailViewModel @Inject constructor(
    val cardReaderManager: CardReaderManager,
    private val tracker: CardReaderTracker,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderDetailFragmentArgs by savedState.navArgs()

    private val viewState = MutableLiveData<ViewState>(Loading)
    val viewStateData: LiveData<ViewState> = viewState

    private lateinit var softwareUpdateAvailabilityJob: Job
    private lateinit var batteryStatusUpdateJob: Job

    init {
        launch {
            cardReaderManager.readerStatus.collect { status ->
                when (status) {
                    is Connected -> {
                        triggerEvent(
                            CardReaderDetailEvent.CardReaderConnected(
                                R.string.card_reader_accessibility_reader_is_connected
                            )
                        )
                        softwareUpdateAvailabilityJob = launch {
                            cardReaderManager.softwareUpdateAvailability.collect(
                                ::handleSoftwareUpdateAvailability
                            )
                        }
                        batteryStatusUpdateJob = launch {
                            cardReaderManager.batteryStatus.collect(
                                ::handleBatteryStatusChange
                            )
                        }
                    }
                    is Connecting -> {
                        handleNotConnectedState()
                    }
                    is NotConnected -> {
                        triggerEvent(
                            CardReaderDetailEvent.CardReaderDisconnected(
                                R.string.card_reader_accessibility_reader_is_disconnected
                            )
                        )
                        handleNotConnectedState()
                    }
                }.exhaustive
            }
        }
    }

    fun onUpdateReaderResult(updateResult: UpdateResult) {
        when (updateResult) {
            SUCCESS -> {
                handleSoftwareUpdateAvailability(SoftwareUpdateAvailability.NotAvailable)
                triggerEvent(Event.ShowSnackbar(R.string.card_reader_detail_connected_update_success))
            }
            FAILED -> triggerEvent(Event.ShowSnackbar(R.string.card_reader_detail_connected_update_failed))
        }.exhaustive
    }

    private fun handleNotConnectedState() {
        cancelConnectedScopeJobs()
        viewState.value =
            NotConnectedState(onPrimaryActionClicked = ::onConnectBtnClicked, onLearnMoreClicked = ::onLearnMoreClicked)
    }

    private fun cancelConnectedScopeJobs() {
        if (::softwareUpdateAvailabilityJob.isInitialized) softwareUpdateAvailabilityJob.cancel()
        if (::batteryStatusUpdateJob.isInitialized) batteryStatusUpdateJob.cancel()
    }

    private fun showConnectedState(readerStatus: Connected, updateAvailable: Boolean = false) {
        viewState.value = if (updateAvailable) {
            ConnectedState(
                enforceReaderUpdate = UiStringRes(
                    R.string.card_reader_detail_connected_enforced_update_software
                ),
                readerName = readerStatus.cardReader.getReadersName(),
                readerBattery = readerStatus.cardReader.getReadersBatteryLevel(),
                readerFirmwareVersion = readerStatus.cardReader.getReaderFirmwareVersion(),
                primaryButtonState = ButtonState(
                    onActionClicked = ::onUpdateReaderClicked,
                    text = UiStringRes(R.string.card_reader_detail_connected_update_software)
                ),
                secondaryButtonState = ButtonState(
                    onActionClicked = ::onDisconnectClicked,
                    text = UiStringRes(R.string.card_reader_detail_connected_disconnect_reader)
                ),
                onReaderNameLongClick = { onReadersNameLongClick(readerStatus.cardReader.id) },
                onLearnMoreClicked = ::onLearnMoreClicked,
            )
        } else {
            ConnectedState(
                enforceReaderUpdate = null,
                readerName = readerStatus.cardReader.getReadersName(),
                readerBattery = readerStatus.cardReader.getReadersBatteryLevel(),
                readerFirmwareVersion = readerStatus.cardReader.getReaderFirmwareVersion(),
                primaryButtonState = ButtonState(
                    onActionClicked = ::onDisconnectClicked,
                    text = UiStringRes(R.string.card_reader_detail_connected_disconnect_reader)
                ),
                secondaryButtonState = null,
                onReaderNameLongClick = { onReadersNameLongClick(readerStatus.cardReader.id) },
                onLearnMoreClicked = ::onLearnMoreClicked,
            )
        }
    }

    private fun updateBatteryLevelOnConnectedState(newBatteryLevel: Float) {
        val currentState = viewState.value
        if (currentState is ConnectedState) {
            viewState.value = currentState.copy(
                readerBattery = buildBatteryLevelUiString(newBatteryLevel)
            )
        }
    }

    private fun onLearnMoreClicked() {
        val preferredPlugin = appPrefsWrapper.getCardReaderPreferredPlugin(
            selectedSite.get().id,
            selectedSite.get().siteId,
            selectedSite.get().selfHostedSiteId
        )
        val learnMoreUrl = when (preferredPlugin) {
            STRIPE_EXTENSION_GATEWAY -> AppUrls.STRIPE_LEARN_MORE_ABOUT_PAYMENTS
            WOOCOMMERCE_PAYMENTS, null -> AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS
        }
        triggerEvent(NavigateToUrlInGenericWebView(learnMoreUrl))
    }

    private fun onConnectBtnClicked() {
        tracker.trackDiscoveryTapped()
        triggerEvent(CardReaderConnectScreen(arguments.cardReaderFlowParam))
    }

    private fun onUpdateReaderClicked() {
        triggerEvent(NavigationTarget.CardReaderUpdateScreen)
    }

    private fun onDisconnectClicked() {
        tracker.trackDisconnectTapped()
        launch {
            clearLastKnowReader()
            val disconnectionResult = cardReaderManager.disconnectReader()
            if (!disconnectionResult) {
                WooLog.e(WooLog.T.CARD_READER, "Disconnection from reader has failed")
                handleNotConnectedState()
            }
        }
    }

    private fun handleSoftwareUpdateAvailability(updateStatus: SoftwareUpdateAvailability) {
        val readerStatus = cardReaderManager.readerStatus.value
        if (readerStatus !is Connected) return
        when (updateStatus) {
            SoftwareUpdateAvailability.Available -> showConnectedState(readerStatus, updateAvailable = true)
            SoftwareUpdateAvailability.NotAvailable -> showConnectedState(readerStatus)
        }.exhaustive
    }

    private fun handleBatteryStatusChange(newStatus: CardReaderBatteryStatus) {
        when (newStatus) {
            is StatusChanged -> {
                updateBatteryLevelOnConnectedState(newStatus.batteryLevel)
            }
            else -> {}
        }.exhaustive
    }

    private fun clearLastKnowReader() {
        appPrefsWrapper.removeLastConnectedCardReaderId()
    }

    private fun onReadersNameLongClick(readersName: String?) {
        if (readersName.isNullOrBlank()) return
        triggerEvent(CardReaderDetailEvent.CopyReadersNameToClipboard(readersName))
        triggerEvent(Event.ShowSnackbar(R.string.card_reader_detail_connected_readers_name_clipboard))
    }

    sealed class NavigationTarget : Event() {
        data class CardReaderConnectScreen(val cardReaderFlowParam: CardReaderFlowParam) : NavigationTarget()
        object CardReaderUpdateScreen : NavigationTarget()
    }

    sealed class CardReaderDetailEvent : Event() {
        data class NavigateToUrlInGenericWebView(val url: String) : CardReaderDetailEvent()
        data class CopyReadersNameToClipboard(val readersName: String) : CardReaderDetailEvent()
        data class CardReaderDisconnected(
            @StringRes val accessibilityDisconnectedText: Int =
                R.string.card_reader_accessibility_reader_is_disconnected
        ) : CardReaderDetailEvent()
        data class CardReaderConnected(
            @StringRes val accessibilityConnectedText: Int =
                R.string.card_reader_accessibility_reader_is_connected
        ) : CardReaderDetailEvent()
    }

    sealed class ViewState {
        data class NotConnectedState(
            val onPrimaryActionClicked: (() -> Unit),
            val onLearnMoreClicked: (() -> Unit),
        ) : ViewState() {
            val headerLabel = UiStringRes(R.string.card_reader_detail_not_connected_header)

            @DrawableRes
            val illustration = R.drawable.img_card_reader_not_connected
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
            val readerFirmwareVersion: UiString,
            val primaryButtonState: ButtonState?,
            val secondaryButtonState: ButtonState?,
            val onReaderNameLongClick: (() -> Unit),
            val onLearnMoreClicked: (() -> Unit),
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

private fun CardReader.getReadersName(): UiString {
    return with(id) {
        if (isNullOrEmpty()) {
            UiStringRes(R.string.card_reader_detail_connected_reader_unknown)
        } else {
            UiStringText(this)
        }
    }
}

private fun CardReader.getReadersBatteryLevel(): UiString? = currentBatteryLevel?.let { buildBatteryLevelUiString(it) }

private fun CardReader.getReaderFirmwareVersion(): UiString {
    return UiStringRes(
        R.string.card_reader_detail_connected_firmware_version,
        listOf(UiStringText(firmwareVersion))
    )
}

private fun buildBatteryLevelUiString(batteryLevel: Float) = UiStringRes(
    R.string.card_reader_detail_connected_battery_percentage,
    listOf(UiStringText((batteryLevel * PERCENT_100).roundToInt().toString()))
)
