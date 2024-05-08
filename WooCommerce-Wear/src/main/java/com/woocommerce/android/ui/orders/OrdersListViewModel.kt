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
            .onEach {
                requestOrdersData(it)
            }.launchIn(this)
    }

    private fun requestOrdersData(selectedSite: SiteModel) {
        selectedSite.apply {  }
        // TODO: Introduce actual request
        _viewState.update {
            it.copy(orders = listOf(
                OrderListItem(
                    date = "2021-09-01",
                    number = "123",
                    customerName = "John Doe",
                    total = "$100.00",
                    status = "Processing"
                ),
                OrderListItem(
                    date = "2021-09-02",
                    number = "124",
                    customerName = "Jane Doe",
                    total = "$200.00",
                    status = "Completed"
                ),
                OrderListItem(
                    date = "2021-09-03",
                    number = "125",
                    customerName = "John Smith",
                    total = "$300.00",
                    status = "Pending"
                )
            ))
        }
    }

    @Parcelize
    data class ViewState(
        val orders: List<OrderListItem> = emptyList()
    ) : Parcelable

    @Parcelize
    data class OrderListItem(
        val date: String,
        val number: String,
        val customerName: String,
        val total: String,
        val status: String
    ) : Parcelable

    @AssistedFactory
    interface Factory {
        fun create(navController: NavHostController): OrdersListViewModel
    }
}
