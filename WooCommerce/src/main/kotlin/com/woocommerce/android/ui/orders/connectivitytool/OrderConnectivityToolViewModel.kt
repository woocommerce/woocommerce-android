package com.woocommerce.android.ui.orders.connectivitytool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTest.InternetConnectionTest
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTest.StoreConnectionTest
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTest.StoreOrdersTest
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTest.WordPressConnectionTest
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.NotStarted
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OrderConnectivityToolViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init {
        listOf(
            InternetConnectionTest,
            WordPressConnectionTest,
            StoreConnectionTest,
            StoreOrdersTest
        ).forEach { test ->
        }
    }

    data class ViewState(
        val isContactSupportEnabled: Boolean = false,
        val internetConnectionTestStatus: ConnectivityTestStatus = NotStarted,
        val wordpressConnectionTestStatus: ConnectivityTestStatus = NotStarted,
        val storeConnectionTestStatus: ConnectivityTestStatus = NotStarted,
        val storeOrdersTestStatus: ConnectivityTestStatus = NotStarted
    )

    sealed class ConnectivityTest(action: () -> Flow<ConnectivityTestStatus>) {
        data object InternetConnectionTest : ConnectivityTest(
            action = {
                flow {
                    emit(ConnectivityTestStatus.InProgress)
                    emit(ConnectivityTestStatus.Success)
                }
            }
        )
        data object WordPressConnectionTest : ConnectivityTest(
            action = {
                flow {
                    emit(ConnectivityTestStatus.InProgress)
                    emit(ConnectivityTestStatus.Success)
                }
            }
        )
        data object StoreConnectionTest : ConnectivityTest(
            action = {
                flow {
                    emit(ConnectivityTestStatus.InProgress)
                    emit(ConnectivityTestStatus.Success)
                }
            }
        )
        data object StoreOrdersTest : ConnectivityTest(
            action = {
                flow {
                    emit(ConnectivityTestStatus.InProgress)
                    emit(ConnectivityTestStatus.Success)
                }
            }
        )
    }

    enum class ConnectivityTestStatus {
        NotStarted,
        InProgress,
        Success,
        Failure
    }
}
