package com.woocommerce.android.ui.woopos.home.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosProductsViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val priceFormat: WooPosFormatPrice,
    private val appPrefsWrapper: AppPrefsWrapper,
) : ViewModel() {
    private var loadMoreProductsJob: Job? = null

    private val _viewState = MutableStateFlow<WooPosProductsViewState>(WooPosProductsViewState.Loading())
    val viewState: StateFlow<WooPosProductsViewState> = _viewState

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            productsDataSource.products
                .map { products -> calculateViewState(products) }
                .collect { _viewState.value = it }
        }
        viewModelScope.launch {
            val result = productsDataSource.loadSimpleProducts(forceRefreshProducts = false)
            if (result.isFailure) {
                _viewState.value = WooPosProductsViewState.Error(reloadingProducts = false)
            }
        }
    }

    fun onUIEvent(event: WooPosProductsUIEvent) {
        when (event) {
            is WooPosProductsUIEvent.EndOfProductListReached -> {
                onEndOfProductsListReached()
            }

            is WooPosProductsUIEvent.ItemClicked -> {
                onItemClicked(event.item)
            }

            WooPosProductsUIEvent.PullToRefreshTriggered -> {
                reloadProducts()
            }

            WooPosProductsUIEvent.SimpleProductsOnlyBannerClosed -> {
                onBannerClosed()
            }
        }
    }

    private fun onBannerClosed() {
        appPrefsWrapper.isWooPosSimpleProductsOnlyBannerShown = true
        _viewState.value = (_viewState.value as? WooPosProductsViewState.Content)?.copy(
            isSimpleProductsOnlyBannerShown = true
        )!!
    }

    private fun reloadProducts() {
        viewModelScope.launch {
            updateProductsReloadingState(isReloading = true)
            productsDataSource.loadSimpleProducts(forceRefreshProducts = true)
            updateProductsReloadingState(isReloading = false)
        }
    }

    private fun updateProductsReloadingState(isReloading: Boolean) {
        _viewState.value = when (val state = viewState.value) {
            is WooPosProductsViewState.Content -> state.copy(reloadingProducts = isReloading)
            is WooPosProductsViewState.Loading -> state.copy(reloadingProducts = isReloading)
            is WooPosProductsViewState.Error -> state.copy(reloadingProducts = isReloading)
            is WooPosProductsViewState.Empty -> state.copy(reloadingProducts = isReloading)
        }
    }

    private suspend fun calculateViewState(products: List<Product>): WooPosProductsViewState =
        when {
            products.isEmpty() && !isReloadingProducts() -> WooPosProductsViewState.Empty()
            products.isEmpty() && isReloadingProducts() ->
                WooPosProductsViewState.Loading(reloadingProducts = true)
            else -> products.toContentState()
        }

    private suspend fun List<Product>.toContentState() = WooPosProductsViewState.Content(
        products = map { product ->
            WooPosProductsListItem(
                id = product.remoteId,
                name = product.name,
                price = priceFormat(product.price),
                imageUrl = product.firstImageUrl,
            )
        },
        isSimpleProductsOnlyBannerShown = appPrefsWrapper.isWooPosSimpleProductsOnlyBannerShown,
        loadingMore = false,
        reloadingProducts = false,
    )

    private fun isReloadingProducts(): Boolean = viewState.value.reloadingProducts

    private fun onEndOfProductsListReached() {
        val currentState = _viewState.value
        if (currentState !is WooPosProductsViewState.Content) {
            return
        }

        if (!productsDataSource.hasMorePages) {
            return
        }

        _viewState.value = currentState.copy(loadingMore = true)

        loadMoreProductsJob?.cancel()
        loadMoreProductsJob = viewModelScope.launch {
            productsDataSource.loadMore()
        }
    }

    private fun onItemClicked(item: WooPosProductsListItem) {
        viewModelScope.launch {
            fromChildToParentEventSender.sendToParent(
                ChildToParentEvent.ItemClickedInProductSelector(item.id)
            )
        }
    }

    private fun shouldShowProductsOnlyBanner(): Boolean {
        return appPrefsWrapper.isWooPosSimpleProductsOnlyBannerShown
    }
}
