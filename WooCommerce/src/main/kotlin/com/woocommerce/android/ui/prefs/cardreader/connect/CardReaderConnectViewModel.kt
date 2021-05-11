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
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.InitializeCardReaderManager
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ConnectingState
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
    private val dispatchers: CoroutineDispatchers,
    private val appLogWrapper: AppLogWrapper
) : ScopedViewModel(savedState) {
    private lateinit var cardReaderManager: CardReaderManager

    // The app shouldn't store the state as connection flow gets canceled when the vm dies
    private val viewState = MutableLiveData<ViewState>(ScanningState(::onCancelScanningClicked))
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
                    viewState.value = ScanningState(::onCancelScanningClicked)
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
                onSecondaryActionClicked = ::onCancelScanningClicked
            )
        } else {
            viewState.value = ScanningState(::onCancelScanningClicked)
        }
    }

    private fun onConnectToReaderClicked(cardReader: CardReader) {
        viewState.value = ConnectingState(::onCancelScanningClicked)
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

    private fun onCancelScanningClicked() {
        appLogWrapper.e(T.MAIN, "Connection flow interrupted by the user.")
        triggerEvent(Exit)
    }

    private fun onReaderConnected() {
        appLogWrapper.e(T.MAIN, "Connecting to reader succeeded.")
        triggerEvent(Exit)
    }

    sealed class CardReaderConnectEvent : Event() {
        object InitializeCardReaderManager : CardReaderConnectEvent()
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
    }
}
