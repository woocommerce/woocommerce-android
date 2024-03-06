package com.woocommerce.android.ui.orders.connectivitytool

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.NotStarted
import com.woocommerce.android.ui.orders.connectivitytool.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.WordPressConnectionCheckUseCase
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
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
    private val _checkStatus = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = CheckStatus()
    )

    val isContactSupportEnabled = _checkStatus
        .map { it.isCheckFinished }
        .asLiveData()

    val viewState = _checkStatus.asLiveData()

    fun startConnectionTests() {
        launch {
            internetConnectionCheck().onEach {
                _checkStatus.value = _checkStatus.value.copy(internetConnectionTestStatus = it)
            }.launchIn(viewModelScope)

            wordPressConnectionCheck().onEach {
                _checkStatus.value = _checkStatus.value.copy(wordpressConnectionTestStatus = it)
            }.launchIn(viewModelScope)

            storeConnectionCheck().onEach {
                _checkStatus.value = _checkStatus.value.copy(storeConnectionTestStatus = it)
            }.launchIn(viewModelScope)

            storeOrdersCheck().onEach {
                _checkStatus.value = _checkStatus.value.copy(storeOrdersTestStatus = it)
            }.launchIn(viewModelScope)
        }
    }

    @Parcelize
    data class CheckStatus(
        val internetConnectionTestStatus: ConnectivityTestStatus = NotStarted,
        val wordpressConnectionTestStatus: ConnectivityTestStatus = NotStarted,
        val storeConnectionTestStatus: ConnectivityTestStatus = NotStarted,
        val storeOrdersTestStatus: ConnectivityTestStatus = NotStarted
    ) : Parcelable {
        val isCheckFinished
            get() = internetConnectionTestStatus.isFinished() &&
                wordpressConnectionTestStatus.isFinished() &&
                storeConnectionTestStatus.isFinished() &&
                storeOrdersTestStatus.isFinished()
    }

    enum class ConnectivityTestStatus {
        NotStarted,
        InProgress,
        Success,
        Failure;

        fun isFinished() = this == Success || this == Failure
    }
}
