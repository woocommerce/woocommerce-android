package com.woocommerce.android.ui.orders.connectivitytool

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.InternetConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.StoreConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.StoreOrdersConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.WordPressConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.WordPressConnectionCheckUseCase
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderConnectivityToolViewModel @Inject constructor(
    private val internetConnectionCheck: InternetConnectionCheckUseCase,
    private val wordPressConnectionCheck: WordPressConnectionCheckUseCase,
    private val storeConnectionCheck: StoreConnectionCheckUseCase,
    private val storeOrdersCheck: StoreOrdersCheckUseCase,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val checkStatus = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = CheckStatus()
    )
    val viewState = checkStatus.asLiveData()

    fun startConnectionTests() {
        launch {
            internetConnectionCheck().onEach {
                checkStatus.value = checkStatus.value.copy(internetConnectionCheckData = it)
            }.launchIn(viewModelScope)

            wordPressConnectionCheck().onEach {
                checkStatus.value = checkStatus.value.copy(wordpressConnectionCheckData = it)
            }.launchIn(viewModelScope)

            storeConnectionCheck().onEach {
                checkStatus.value = checkStatus.value.copy(storeConnectionCheckData = it)
            }.launchIn(viewModelScope)

            storeOrdersCheck().onEach {
                checkStatus.value = checkStatus.value.copy(storeOrdersCheckData = it)
            }.launchIn(viewModelScope)
        }
    }

    fun onContactSupportClicked() { triggerEvent(OpenSupportRequest) }

    @Parcelize
    data class CheckStatus(
        val internetConnectionCheckData: InternetConnectivityCheckData = InternetConnectivityCheckData(),
        val wordpressConnectionCheckData: WordPressConnectivityCheckData = WordPressConnectivityCheckData(),
        val storeConnectionCheckData: StoreConnectivityCheckData = StoreConnectivityCheckData(),
        val storeOrdersCheckData: StoreOrdersConnectivityCheckData = StoreOrdersConnectivityCheckData()
    ) : Parcelable {
        val isCheckFinished
            get() = internetConnectionCheckData.isFinished &&
                wordpressConnectionCheckData.isFinished &&
                storeConnectionCheckData.isFinished &&
                storeOrdersCheckData.isFinished
    }

    object OpenSupportRequest : MultiLiveEvent.Event()
}
