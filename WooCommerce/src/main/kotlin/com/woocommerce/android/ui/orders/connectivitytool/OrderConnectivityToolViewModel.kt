package com.woocommerce.android.ui.orders.connectivitytool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTest.InternetConnectionTest
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTest.StoreConnectionTest
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTest.StoreOrdersTest
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTest.WordPressConnectionTest
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.NotStarted
import com.woocommerce.android.ui.orders.connectivitytool.useCases.InternetConnectionTestUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreConnectionTestUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreOrdersTestUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.WordPressConnectionTestUseCase
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class OrderConnectivityToolViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init {
        launch {
            InternetConnectionTest.run().collect {
                _viewState.value = _viewState.value.copy(internetConnectionTestStatus = it)
            }
            WordPressConnectionTest.run().collect {
                _viewState.value = _viewState.value.copy(wordpressConnectionTestStatus = it)
            }
            StoreConnectionTest.run().collect {
                _viewState.value = _viewState.value.copy(storeConnectionTestStatus = it)
            }
            StoreOrdersTest.run().collect {
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

    sealed class ConnectivityTest(val action: () -> Flow<ConnectivityTestStatus>) {
        fun run() = action.invoke()

        data object InternetConnectionTest : ConnectivityTest(
            action = { InternetConnectionTestUseCase().invoke() }
        )
        data object WordPressConnectionTest : ConnectivityTest(
            action = { WordPressConnectionTestUseCase().invoke() }
        )
        data object StoreConnectionTest : ConnectivityTest(
            action = { StoreConnectionTestUseCase().invoke() }
        )
        data object StoreOrdersTest : ConnectivityTest(
            action = { StoreOrdersTestUseCase().invoke() }
        )
    }

    enum class ConnectivityTestStatus {
        NotStarted,
        InProgress,
        Success,
        Failure
    }
}
