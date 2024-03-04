package com.woocommerce.android.ui.orders.connectivitytool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.NotStarted
import com.woocommerce.android.ui.orders.connectivitytool.useCases.InternetConnectionTestUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreConnectionTestUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreOrdersTestUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.WordPressConnectionTestUseCase
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import javax.inject.Inject
import kotlinx.coroutines.launch

class OrderConnectivityToolViewModel @Inject constructor(
    private val internetConnectionTest: InternetConnectionTestUseCase,
    private val wordPressConnectionTest: WordPressConnectionTestUseCase,
    private val storeConnectionTest: StoreConnectionTestUseCase,
    private val storeOrdersTest: StoreOrdersTestUseCase,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init {
        launch {
            internetConnectionTest().collect {
                _viewState.value = _viewState.value.copy(internetConnectionTestStatus = it)
            }
            wordPressConnectionTest().collect {
                _viewState.value = _viewState.value.copy(wordpressConnectionTestStatus = it)
            }
            storeConnectionTest().collect {
                _viewState.value = _viewState.value.copy(storeConnectionTestStatus = it)
            }
            storeOrdersTest().collect {
                _viewState.value = _viewState.value.copy(storeOrdersTestStatus = it)
            }
        }
    }

    data class ViewState(
        val isContactSupportEnabled: Boolean = false,
        val internetConnectionTestStatus: ConnectivityTestStatus = NotStarted,
        val wordpressConnectionTestStatus: ConnectivityTestStatus = NotStarted,
        val storeConnectionTestStatus: ConnectivityTestStatus = NotStarted,
        val storeOrdersTestStatus: ConnectivityTestStatus = NotStarted
    )

    enum class ConnectivityTestStatus {
        NotStarted,
        InProgress,
        Success,
        Failure
    }
}
