package com.woocommerce.android.ui.orders

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.navigation.NavHostController
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.commons.viewmodel.ScopedViewModel
import com.woocommerce.commons.viewmodel.getStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel

@Suppress("UnusedPrivateProperty")
@HiltViewModel(assistedFactory = OrdersListViewModel.Factory::class)
class OrdersListViewModel @AssistedInject constructor(
    @Assisted private val navController: NavHostController,
    private val fetchOrders: FetchOrders,
    loginRepository: LoginRepository,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init {
        loginRepository.selectedSiteFlow
            .filterNotNull()
            .onEach { requestOrdersData(it) }
            .launchIn(this)
    }

    private suspend fun requestOrdersData(selectedSite: SiteModel) {
        _viewState.update { it.copy(isLoading = true) }
        fetchOrders(selectedSite)
            .onEach { orders ->
                _viewState.update {
                    it.copy(
                        orders = orders,
                        isLoading = false
                    )
                }
            }.launchIn(this)

    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = false,
        val orders: List<OrderItem> = emptyList()
    ) : Parcelable

    @Parcelize
    data class OrderItem(
        val date: String,
        val number: String,
        val customerName: String?,
        val total: String,
        val status: String
    ) : Parcelable

    @AssistedFactory
    interface Factory {
        fun create(navController: NavHostController): OrdersListViewModel
    }
}
