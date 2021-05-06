package com.woocommerce.android.ui.prefs.cardreader.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.LoadingDataState
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderConnectScreen
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.NotConnectedState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    private val viewState = MutableLiveData<ViewState>(
        NotConnectedState
    )
    val viewStateData: LiveData<ViewState> = viewState

    fun onConnectBtnClicked() {
        triggerEvent(CardReaderConnectScreen)
    }

    sealed class NavigationTarget : Event() {
        object CardReaderConnectScreen : NavigationTarget()
    }

    sealed class ViewState(
        val headerLabel: Int? = null
    ) {
        object NotConnectedState: ViewState(
            headerLabel = R.string.card_reader_detail_not_connected_header
        )
    }
}
