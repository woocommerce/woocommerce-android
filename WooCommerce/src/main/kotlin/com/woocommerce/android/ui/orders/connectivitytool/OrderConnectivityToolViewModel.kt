package com.woocommerce.android.ui.orders.connectivitytool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.InternetConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.StoreConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.StoreOrdersConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.WordPressConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStep.Finished
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStep.InternetCheck
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStep.StoreCheck
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStep.StoreOrdersCheck
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStep.WordPressCheck
import com.woocommerce.android.ui.orders.connectivitytool.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.WordPressConnectionCheckUseCase
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.map

@HiltViewModel
class OrderConnectivityToolViewModel @Inject constructor(
    private val internetConnectionCheck: InternetConnectionCheckUseCase,
    private val wordPressConnectionCheck: WordPressConnectionCheckUseCase,
    private val storeConnectionCheck: StoreConnectionCheckUseCase,
    private val storeOrdersCheck: StoreOrdersCheckUseCase,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val stateMachine = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = InternetCheck
    )

    private val internetCheckFlow = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = InternetConnectivityCheckData()
    )
    val internetCheckData = internetCheckFlow.asLiveData()

    private val wordpressCheckFlow = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = WordPressConnectivityCheckData()
    )
    val wordpressCheckData = wordpressCheckFlow.asLiveData()

    private val storeCheckFlow = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = StoreConnectivityCheckData()
    )
    val storeCheckData = storeCheckFlow.asLiveData()

    private val ordersCheckFlow = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = StoreOrdersConnectivityCheckData()
    )
    val storeOrdersCheckData = ordersCheckFlow.asLiveData()

    val isCheckFinished = stateMachine.map { it == Finished }.asLiveData()

    fun startConnectionTests() {
        launch {
            stateMachine.collect {
                when (it) {
                    InternetCheck -> startInternetCheck()
                    WordPressCheck -> startWordPressCheck()
                    StoreCheck -> startStoreCheck()
                    StoreOrdersCheck -> startStoreOrdersCheck()
                    Finished -> { /* No-op */ }
                }
            }
        }
    }

    private fun startStoreOrdersCheck() {
        storeOrdersCheck().onEach { status ->
            ordersCheckFlow.update {
                it.copy(connectivityCheckStatus = status)
            }
        }.launchIn(viewModelScope)
    }

    private fun startStoreCheck() {
        storeConnectionCheck().onEach { status ->
            storeCheckFlow.update {
                it.copy(connectivityCheckStatus = status)
            }
        }.launchIn(viewModelScope)
    }

    private fun startWordPressCheck() {
        wordPressConnectionCheck().onEach { status ->
            wordpressCheckFlow.update {
                it.copy(connectivityCheckStatus = status)
            }
        }.launchIn(viewModelScope)
    }

    private fun startInternetCheck() {
        internetConnectionCheck().onEach { status ->
            internetCheckFlow.update {
                it.copy(connectivityCheckStatus = status)
            }
        }.launchIn(viewModelScope)
    }

    fun onContactSupportClicked() { triggerEvent(OpenSupportRequest) }

    object OpenSupportRequest : MultiLiveEvent.Event()

    enum class ConnectivityCheckStep {
        InternetCheck,
        WordPressCheck,
        StoreCheck,
        StoreOrdersCheck,
        Finished
    }
}
