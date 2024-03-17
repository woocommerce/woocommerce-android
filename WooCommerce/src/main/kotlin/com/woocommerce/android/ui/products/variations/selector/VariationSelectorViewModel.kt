package com.woocommerce.android.ui.products.variations.selector

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.isInteger
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductStockStatus.Custom
import com.woocommerce.android.ui.products.ProductStockStatus.InStock
import com.woocommerce.android.ui.products.ProductStockStatus.NotAvailable
import com.woocommerce.android.ui.products.selector.ProductSelectorTracker
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.ui.products.selector.ProductSourceForTracking
import com.woocommerce.android.ui.products.selector.SelectionState
import com.woocommerce.android.ui.products.selector.SelectionState.SELECTED
import com.woocommerce.android.ui.products.selector.SelectionState.UNSELECTED
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel.LoadingState.APPENDING
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel.LoadingState.IDLE
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel.LoadingState.LOADING
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class VariationSelectorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: VariationSelectorRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val variationListHandler: VariationListHandler,
    private val resourceProvider: ResourceProvider,
    private val tracker: ProductSelectorTracker,
) : ScopedViewModel(savedState) {
    companion object {
        private const val STATE_UPDATE_DELAY = 100L
    }

    private val currencyCode by lazy {
        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    }

    private val navArgs: VariationSelectorFragmentArgs by savedState.navArgs()
    private val productSelectorFlow = navArgs.productSelectorFlow

    private val loadingState = MutableStateFlow(IDLE)
    private val selectedVariationIds = savedState.getStateFlow(viewModelScope, navArgs.variationIds.toSet())
    private val product: Deferred<Product?> = async {
        repository.getProduct(navArgs.productId)
    }
    val screenMode = navArgs.screenMode

    val viewSate = combine(
        variationListHandler.getVariationsFlow(navArgs.productId),
        loadingState.withIndex()
            .debounce {
                if (it.index != 0 && it.value == IDLE) {
                    // When resetting to IDLE, wait a bit to make sure the list has been fetched from DB
                    STATE_UPDATE_DELAY
                } else {
                    0L
                }
            }
            .map { it.value },
        selectedVariationIds
    ) { variations, loadingState, selectedIds ->
        ViewState(
            loadingState = loadingState,
            productName = product.await()?.name ?: "",
            variations = variations.map { it.toUiModel(selectedIds) },
            selectedItemsCount = selectedIds.size
        )
    }.asLiveData()

    init {
        viewModelScope.launch {
            loadingState.value = LOADING
            variationListHandler.fetchVariations(productId = navArgs.productId, forceRefresh = true)
            loadingState.value = IDLE
        }
    }

    private suspend fun ProductVariation.toUiModel(selectedIds: Set<Long>): VariationListItem {
        val stockStatus = when (stockStatus) {
            InStock -> {
                if (isStockManaged) {
                    val quantity = if (stockQuantity.isInteger()) stockQuantity.toInt() else stockQuantity
                    resourceProvider.getString(string.product_stock_status_instock_quantified, quantity.toString())
                } else {
                    resourceProvider.getString(string.product_stock_status_instock)
                }
            }
            NotAvailable, is Custom -> null
            else -> resourceProvider.getString(stockStatus.stringResource)
        }

        val price = price?.let { PriceUtils.formatCurrency(price, currencyCode, currencyFormatter) }

        val stockAndPrice = listOfNotNull(stockStatus, price).joinToString(" \u2022 ")

        return VariationListItem(
            id = remoteVariationId,
            title = getName(product.await()),
            imageUrl = image?.source,
            sku = sku.takeIf { it.isNotBlank() },
            stockAndPrice = stockAndPrice,
            selectionState = if (selectedIds.contains(remoteVariationId)) SELECTED else UNSELECTED
        )
    }

    fun onClearButtonClick() {
        launch {
            trackClearSelectionButtonClicked()
            delay(STATE_UPDATE_DELAY) // let the animation play out before hiding the button
            selectedVariationIds.value = emptySet()
        }
    }

    private fun trackClearSelectionButtonClicked() {
        when (navArgs.productSelectorFlow) {
            ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ProductSelectorViewModel.ProductSelectorFlow.OrderEditing -> {
                tracker.trackClearSelectionButtonClicked(
                    productSelectorFlow,
                    ProductSelectorTracker.ProductSelectorSource.VariationSelector
                )
            }
            ProductSelectorViewModel.ProductSelectorFlow.CouponEdition -> {}
            ProductSelectorViewModel.ProductSelectorFlow.Undefined -> {}
        }
    }

    fun onVariationClick(item: VariationListItem) {
        if (selectedVariationIds.value.contains(item.id)) {
            trackVariationUnselected()
            selectedVariationIds.value = selectedVariationIds.value - item.id
        } else {
            trackVariationSelected()
            selectedVariationIds.value = selectedVariationIds.value + item.id
        }
    }

    private fun trackVariationSelected() {
        when (navArgs.productSelectorFlow) {
            ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ProductSelectorViewModel.ProductSelectorFlow.OrderEditing -> {
                tracker.trackItemSelected(productSelectorFlow)
            }
            ProductSelectorViewModel.ProductSelectorFlow.CouponEdition -> {}
            ProductSelectorViewModel.ProductSelectorFlow.Undefined -> {}
        }
    }

    private fun trackVariationUnselected() {
        when (navArgs.productSelectorFlow) {
            ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            ProductSelectorViewModel.ProductSelectorFlow.OrderEditing -> {
                tracker.trackItemUnselected(productSelectorFlow)
            }
            ProductSelectorViewModel.ProductSelectorFlow.CouponEdition -> {}
            ProductSelectorViewModel.ProductSelectorFlow.Undefined -> {}
        }
    }

    fun onLoadMore() {
        viewModelScope.launch {
            loadingState.value = APPENDING
            variationListHandler.loadMore(navArgs.productId)
            loadingState.value = IDLE
        }
    }

    fun onBackPress() {
        triggerEvent(
            ExitWithResult(
                VariationSelectionResult(navArgs.productId, selectedVariationIds.value, navArgs.productSource)
            )
        )
    }

    data class ViewState(
        val loadingState: LoadingState = IDLE,
        val productName: String = "",
        val variations: List<VariationListItem> = emptyList(),
        val selectedItemsCount: Int = 0
    )

    data class VariationListItem(
        val id: Long,
        val title: String,
        val imageUrl: String? = null,
        val stockAndPrice: String? = null,
        val sku: String? = null,
        val selectionState: SelectionState = UNSELECTED
    )

    @Parcelize
    data class VariationSelectionResult(
        val productId: Long,
        val selectedVariationIds: Set<Long>,
        val productSourceForTracking: ProductSourceForTracking,
    ) : Parcelable

    enum class LoadingState {
        IDLE, LOADING, APPENDING
    }

    enum class ScreenMode {
        DIALOG, FULLSCREEN
    }
}
