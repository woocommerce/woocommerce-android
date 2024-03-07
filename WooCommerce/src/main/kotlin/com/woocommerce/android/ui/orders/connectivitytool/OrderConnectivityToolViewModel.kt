package com.woocommerce.android.ui.orders.connectivitytool

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityCheckStatus.NotStarted
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
                checkStatus.value = checkStatus.value.copy(internetConnectionCheckStatus = it)
            }.launchIn(viewModelScope)

            wordPressConnectionCheck().onEach {
                checkStatus.value = checkStatus.value.copy(wordpressConnectionCheckStatus = it)
            }.launchIn(viewModelScope)

            storeConnectionCheck().onEach {
                checkStatus.value = checkStatus.value.copy(storeConnectionCheckStatus = it)
            }.launchIn(viewModelScope)

            storeOrdersCheck().onEach {
                checkStatus.value = checkStatus.value.copy(storeOrdersCheckStatus = it)
            }.launchIn(viewModelScope)
        }
    }

    fun onContactSupportClicked() { triggerEvent(OpenSupportRequest) }

    @Parcelize
    data class CheckStatus(
        val internetConnectionCheckStatus: ConnectivityCheckStatus = NotStarted,
        val wordpressConnectionCheckStatus: ConnectivityCheckStatus = NotStarted,
        val storeConnectionCheckStatus: ConnectivityCheckStatus = NotStarted,
        val storeOrdersCheckStatus: ConnectivityCheckStatus = NotStarted
    ) : Parcelable {
        val isCheckFinished
            get() = internetConnectionCheckStatus.isFinished() &&
                wordpressConnectionCheckStatus.isFinished() &&
                storeConnectionCheckStatus.isFinished() &&
                storeOrdersCheckStatus.isFinished()
    }

    enum class ConnectivityCheckStatus {
        NotStarted,
        InProgress,
        Success,
        Failure;

        fun isFinished() = this == Success || this == Failure
    }

    object OpenSupportRequest : MultiLiveEvent.Event()
}
