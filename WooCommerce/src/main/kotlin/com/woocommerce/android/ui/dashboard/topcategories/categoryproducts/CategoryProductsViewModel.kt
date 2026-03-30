package com.woocommerce.android.ui.dashboard.topcategories.categoryproducts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.dashboard.TopPerformerProductUiModel
import com.woocommerce.android.ui.dashboard.domain.GetProductsByCategory
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformers.TopPerformerProduct
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.commons.stats.StatsTimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CategoryProductsViewModel @Inject constructor(
    private val getProductsByCategory: GetProductsByCategory,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    private val categoryId: Long = savedState["categoryId"] ?: 0L
    private val categoryName: String = savedState["categoryName"] ?: ""
    private val startDateMillis: Long = savedState["startDateMillis"] ?: 0L
    private val endDateMillis: Long = savedState["endDateMillis"] ?: 0L

    private val _state = MutableStateFlow(
        CategoryProductsState(
            categoryName = categoryName,
            isLoading = true
        )
    )
    val state = _state.asLiveData()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            if (!networkStatus.isConnected()) {
                _state.update { it.copy(isLoading = false, isError = true) }
                return@launch
            }

            _state.update { it.copy(isLoading = true, isError = false) }

            val range = StatsTimeRange(
                Date(startDateMillis),
                Date(endDateMillis)
            )
            getProductsByCategory(range, categoryId).fold(
                onSuccess = { products ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = products.map { product -> product.toUiModel() }
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(isLoading = false, isError = true) }
                }
            )
        }
    }

    fun onRetryClicked() {
        loadProducts()
    }

    private fun onProductTapped(productId: Long) {
        triggerEvent(OpenProduct(productId))
    }

    private fun TopPerformerProduct.toUiModel() =
        TopPerformerProductUiModel(
            productId = productId,
            name = StringEscapeUtils.unescapeHtml4(name),
            timesOrdered = FormatUtils.formatDecimal(quantity),
            netSales = resourceProvider.getString(
                R.string.dashboard_top_performers_net_sales,
                getTotalSpendFormatted(total.toBigDecimal(), currency)
            ),
            imageUrl = imageUrl?.let {
                PhotonUtils.getPhotonImageUrl(
                    it,
                    resourceProvider.getDimensionPixelSize(R.dimen.image_minor_100),
                    0
                )
            },
            onClick = ::onProductTapped
        )

    private fun getTotalSpendFormatted(totalSpend: BigDecimal, currency: String) =
        currencyFormatter.formatCurrency(
            totalSpend,
            wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: currency
        )

    data class CategoryProductsState(
        val categoryName: String = "",
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val products: List<TopPerformerProductUiModel> = emptyList()
    )

    data class OpenProduct(val productId: Long) : MultiLiveEvent.Event()
}
