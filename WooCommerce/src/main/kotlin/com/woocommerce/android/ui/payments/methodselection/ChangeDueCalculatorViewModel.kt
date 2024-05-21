package com.woocommerce.android.ui.payments.methodselection

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ChangeDueCalculatorViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val currencyFormatter: CurrencyFormatter,
    savedStateHandle: SavedStateHandle,
    private val wooCommerceStore: WooCommerceStore,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedStateHandle) {

    private val orderId: Long = savedStateHandle.get<Long>("orderId")
        ?: throw IllegalArgumentException("OrderId is required")

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val amountDue: String, val change: BigDecimal) : UiState()
        data object Error : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadOrderDetails()
    }

    private fun loadOrderDetails() {
        launch {
            val order = orderDetailRepository.getOrderById(orderId)!!
            _uiState.value = UiState.Success(amountDue = formatOrderTotal(order.total), 0.00.toBigDecimal())
        }
    }

    private fun formatOrderTotal(total: BigDecimal): String {
        val currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: ""
        return currencyFormatter.formatCurrency(total, currencyCode)
    }
}
