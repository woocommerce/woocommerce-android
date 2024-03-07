package com.woocommerce.android.ui.orders.connectivitytool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.InternetConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.StoreConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.StoreOrdersConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckCardData.WordPressConnectivityCheckData
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Success
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    private val nextStep
        get() = when (stateMachine.value) {
            InternetCheck -> WordPressCheck
            WordPressCheck -> StoreCheck
            StoreCheck -> StoreOrdersCheck
            StoreOrdersCheck -> Finished
            Finished -> throw IllegalStateException("Cannot move to next state from Finished")
        }

    fun startConnectionChecks() {
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

    fun onContactSupportClicked() { triggerEvent(OpenSupportRequest) }

    private fun startInternetCheck() {
        internetConnectionCheck().onEach { status ->
            internetCheckFlow.update {
                status.startNextCheck()
                it.copy(connectivityCheckStatus = status)
            }
        }.launchIn(viewModelScope)
    }

    private fun startWordPressCheck() {
        wordPressConnectionCheck().onEach { status ->
            wordpressCheckFlow.update {
                status.startNextCheck()
                it.copy(connectivityCheckStatus = status)
            }
        }.launchIn(viewModelScope)
    }

    private fun startStoreCheck() {
        storeConnectionCheck().onEach { status ->
            storeCheckFlow.update {
                status.startNextCheck()
                it.copy(connectivityCheckStatus = status)
            }
        }.launchIn(viewModelScope)
    }

    private fun startStoreOrdersCheck() {
        storeOrdersCheck().onEach { status ->
            status.startNextCheck()
            ordersCheckFlow.update {
                it.copy(connectivityCheckStatus = status)
            }
        }.launchIn(viewModelScope)
    }

    private fun ConnectivityCheckStatus.startNextCheck() {
        if (stateMachine.value == Finished) return

        stateMachine.update {
            when (this) {
                is Success -> nextStep
                is Failure -> Finished
                else -> it
            }
        }
    }

    object OpenSupportRequest : MultiLiveEvent.Event()

    enum class ConnectivityCheckStep {
        InternetCheck,
        WordPressCheck,
        StoreCheck,
        StoreOrdersCheck,
        Finished
    }
}
