package com.woocommerce.android.ui.prefs.cardreader.connect

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.InitializeCardReaderManager
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ScanningState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.DaggerScopedViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.wordpress.android.fluxc.utils.AppLogWrapper

class CardReaderConnectViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val appLogWrapper: AppLogWrapper
) : DaggerScopedViewModel(savedState, dispatchers) {
    private lateinit var cardReaderManager: CardReaderManager

    // The app shouldn't store the state as connection flow gets canceled when the vm dies
    private val viewState = MutableLiveData<ViewState>(ScanningState(::onCancelScanningClicked))
    val viewStateData: LiveData<ViewState> = viewState

    // TODO cardreader replace start with init when DI for CardReaderManager is supported
    fun start(cardReaderManager: CardReaderManager) {
        triggerEvent(InitializeCardReaderManager(cardReaderManager))
    }

    fun onCardReaderManagerInitialized() {
        // TODO cardreader check location permissions
        startScanning()
    }

    private fun onCancelScanningClicked() {
        // TODO cardreader implement
    }

    private fun startScanning() {

    }

    sealed class CardReaderConnectEvent : Event() {
        data class InitializeCardReaderManager(val cardReaderManager: CardReaderManager) : CardReaderConnectEvent()
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
            headerLabel = R.string.card_reader_connect_scanning_header,
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

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<CardReaderConnectViewModel>
}
