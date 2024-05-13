package com.woocommerce.android.ui.orders.list

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.navigation.NavHostController
import com.woocommerce.android.ui.NavRoutes.ORDER_DETAILS
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.android.util.DateUtils
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
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.Locale

@Suppress("LongParameterList")
@HiltViewModel(assistedFactory = OrdersListViewModel.Factory::class)
class OrdersListViewModel @AssistedInject constructor(
    @Assisted private val navController: NavHostController,
    private val fetchOrders: FetchOrders,
    private val wooCommerceStore: WooCommerceStore,
    private val dateUtils: DateUtils,
    private val locale: Locale,
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

    fun onOrderItemClick(orderId: Long) {
        navController.navigate(ORDER_DETAILS.withArgs(orderId))
    }

    private suspend fun requestOrdersData(selectedSite: SiteModel) {
        _viewState.update { it.copy(isLoading = true) }
        fetchOrders(selectedSite)
            .onEach { orders ->
                _viewState.update { viewState ->
                    viewState.copy(
                        orders = orders.map {
                            it.toOrderItem(selectedSite)
                        },
                        isLoading = false
                    )
                }
            }.launchIn(this)
    }

    private fun OrderEntity.toOrderItem(
        selectedSite: SiteModel
    ): OrderItem {
        val formattedOrderTotals = wooCommerceStore.formatCurrencyForDisplay(
            amount = total.toDoubleOrNull() ?: 0.0,
            site = selectedSite,
            currencyCode = null,
            applyDecimalFormatting = true
        )

        val formattedCreationDate = dateUtils.getFormattedDateWithSiteTimeZone(
            dateCreated
        ) ?: dateCreated

        val formattedBillingName = takeUnless {
            billingFirstName.isEmpty() && billingLastName.isEmpty()
        }?.let { "$billingFirstName $billingLastName" }

        val formattedStatus = status.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }

        return OrderItem(
            id = orderId,
            date = formattedCreationDate,
            number = number,
            customerName = formattedBillingName,
            total = formattedOrderTotals,
            status = formattedStatus
        )
    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = false,
        val orders: List<OrderItem> = emptyList()
    ) : Parcelable

    @Parcelize
    data class OrderItem(
        val id: Long,
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
