package com.woocommerce.android.ui.orders.connectivitytool

import android.os.Parcelable
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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@HiltViewModel
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
            internetConnectionTest().onEach {
                _viewState.value = _viewState.value.copy(internetConnectionTestStatus = it)
            }.launchIn(viewModelScope)

            wordPressConnectionTest().onEach {
                _viewState.value = _viewState.value.copy(wordpressConnectionTestStatus = it)
            }.launchIn(viewModelScope)

            storeConnectionTest().onEach {
                _viewState.value = _viewState.value.copy(storeConnectionTestStatus = it)
            }.launchIn(viewModelScope)

            storeOrdersTest().onEach {
                _viewState.value = _viewState.value.copy(storeOrdersTestStatus = it)
            }.launchIn(viewModelScope)
        }
    }

    @Parcelize
    data class ViewState(
        val isContactSupportEnabled: Boolean = false,
        val internetConnectionTestStatus: ConnectivityTestStatus = NotStarted,
        val wordpressConnectionTestStatus: ConnectivityTestStatus = NotStarted,
        val storeConnectionTestStatus: ConnectivityTestStatus = NotStarted,
        val storeOrdersTestStatus: ConnectivityTestStatus = NotStarted
    ) : Parcelable

    enum class ConnectivityTestStatus {
        NotStarted,
        InProgress,
        Success,
        Failure
    }
}
