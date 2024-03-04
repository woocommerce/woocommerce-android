package com.woocommerce.android.ui.orders.connectivitytool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import javax.inject.Inject

class OrderConnectivityToolViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    data class ViewState(
        val isContactSupportEnabled: Boolean = false
    )

    sealed class ConnectivityTest {
        data object InternetConnectionTest : ConnectivityTest()
        data object WordPressConnectionTest : ConnectivityTest()
        data object StoreConnectionTest : ConnectivityTest()
        data object StoreOrdersTest : ConnectivityTest()
    }

    enum class ConnectivityTestStatus {
        NotStarted,
        InProgress,
        Success,
        Failure
    }
}
