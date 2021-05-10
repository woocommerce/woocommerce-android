package com.woocommerce.android.ui.prefs.cardreader.connect

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReader
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.Failed
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.ReadersFound
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.Started
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.Succeeded
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.CheckLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.InitializeCardReaderManager
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.OpenPermissionsSettings
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.RequestLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ConnectingState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.MissingPermissionsError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ReaderFoundState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ScanningState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject

@HiltViewModel
class CardReaderConnectViewModel @Inject constructor(
    savedState: SavedStateHandle,
    dispatchers: CoroutineDispatchers,
    private val appLogWrapper: AppLogWrapper
) : ScopedViewModel(savedState, dispatchers) {
    private lateinit var cardReaderManager: CardReaderManager

    // The app shouldn't store the state as connection flow gets canceled when the vm dies
    private val viewState = MutableLiveData<ViewState>(ScanningState(::onCancelClicked))
    val viewStateData: LiveData<ViewState> = viewState

    init {
        triggerEvent(InitializeCardReaderManager)
    }

    fun onCardReaderManagerInitialized(cardReaderManager: CardReaderManager) {
        this.cardReaderManager = cardReaderManager
        // TODO cardreader check location permissions
        viewModelScope.launch {
            startScanning()
        }
    }

    private suspend fun startScanning() {
        cardReaderManager
            // TODO cardreader set isSimulated to false or add a temporary checkbox to the UI
            .discoverReaders(isSimulated = true)
            // TODO cardreader should we move flowOn to CardReaderModule?
            .flowOn(dispatchers.io)
            .collect { discoveryEvent ->
                handleScanEvent(discoveryEvent)
            }
    }

    private fun handleScanEvent(discoveryEvent: CardReaderDiscoveryEvents) {
        when (discoveryEvent) {
            Started -> {
                if (viewState.value !is ScanningState) {
                    viewState.value = ScanningState(::onCancelClicked)
                }
            }
            is ReadersFound -> onReadersFound(discoveryEvent)
            Succeeded -> {
                // noop
            }
            is Failed -> {
                // TODO cardreader Replace with failed state
                appLogWrapper.e(T.MAIN, "Scanning failed.")
                triggerEvent(Exit)
            }
        }
    }

    private fun onReadersFound(discoveryEvent: ReadersFound) {
        if (viewState.value is ConnectingState) return
        val availableReaders = discoveryEvent.list.filter { it.getId() != null }
        if (availableReaders.size > 0) {
            // TODO cardreader add support for showing multiple readers
            val reader = availableReaders[0]
            viewState.value = ReaderFoundState(
                onPrimaryActionClicked = { onConnectToReaderClicked(reader) },
                onSecondaryActionClicked = ::onCancelClicked
            )
        } else {
            viewState.value = ScanningState(::onCancelClicked)
        }
    }

    private fun onConnectToReaderClicked(cardReader: CardReader) {
        viewState.value = ConnectingState(::onCancelClicked)
        viewModelScope.launch {
            val success = cardReaderManager.connectToReader(cardReader)
            if (success) {
                onReaderConnected()
            } else {
                // TODO cardreader Replace with failed state
                appLogWrapper.e(T.MAIN, "Connecting to reader failed.")
                triggerEvent(Exit)
            }
        }
    }

    private fun onCancelClicked() {
        appLogWrapper.e(T.MAIN, "Connection flow interrupted by the user.")
        triggerEvent(Exit)
    }

    private fun onReaderConnected() {
        appLogWrapper.e(T.MAIN, "Connecting to reader succeeded.")
        triggerEvent(Exit)
    }

    sealed class CardReaderConnectEvent : Event() {
        object InitializeCardReaderManager : CardReaderConnectEvent()
        object CheckLocationPermissions : CardReaderConnectEvent()
        object RequestLocationPermissions : CardReaderConnectEvent()
        object OpenPermissionsSettings : CardReaderConnectEvent()
    }

    sealed class ViewState(
        @StringRes val headerLabel: Int? = null,
        @DrawableRes val illustration: Int? = null,
        @StringRes val hintLabel: Int? = null,
        val primaryActionLabel: Int? = null,
        val secondaryActionLabel: Int? = null
    ) {
        open val onPrimaryActionClicked: (() -> Unit)? = null
        open val onSecondaryActionClicked: (() -> Unit)? = null

        data class ScanningState(override val onSecondaryActionClicked: (() -> Unit)) : ViewState(
            headerLabel = R.string.card_reader_connect_scanning_header,
            illustration = R.drawable.img_card_reader_scanning,
            hintLabel = R.string.card_reader_connect_scanning_hint,
            secondaryActionLabel = R.string.cancel
        )

        data class ReaderFoundState(
            override val onPrimaryActionClicked: (() -> Unit),
            override val onSecondaryActionClicked: (() -> Unit)
        ) : ViewState(
            // TODO cardreader add reader name -> migrate to UiString -> check UiStringResWithParams in WPAndroid
            headerLabel = R.string.card_reader_connect_reader_found_header,
            illustration = R.drawable.img_card_reader,
            primaryActionLabel = R.string.card_reader_connect_to_reader,
            secondaryActionLabel = R.string.cancel
        )

        // TODO cardreader add multiple readers found state

        data class ConnectingState(override val onSecondaryActionClicked: (() -> Unit)) : ViewState(
            headerLabel = R.string.card_reader_connect_connecting_header,
            illustration = R.drawable.img_card_reader_connecting,
            hintLabel = R.string.card_reader_connect_connecting_hint,
            secondaryActionLabel = R.string.cancel
        )

        // TODO cardreader add error state
        data class MissingPermissionsError(
            override val onPrimaryActionClicked: () -> Unit,
            override val onSecondaryActionClicked: () -> Unit
        ) : ViewState(
            headerLabel = R.string.card_reader_connect_failed_header,
            illustration = R.drawable.img_card_reader_scanning,
            hintLabel = R.string.card_reader_connect_missing_permissions,
            primaryActionLabel = R.string.card_reader_connect_open_permission_settings,
            secondaryActionLabel = R.string.cancel
        )
    }
}
