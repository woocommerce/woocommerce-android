package com.woocommerce.android.ui.woopos.home.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.util.datastore.PosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WooPosProductsViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val priceFormat: WooPosFormatPrice,
    private val posPreferencesRepository: PosPreferencesRepository
) : ViewModel() {
    private var loadMoreProductsJob: Job? = null

    private val _viewState = MutableStateFlow<WooPosProductsViewState>(WooPosProductsViewState.Loading())
    val viewState: StateFlow<WooPosProductsViewState> = _viewState

    init {
        loadProducts()
        observeSimpleProductsOnlyBanner()
    }

    private fun observeSimpleProductsOnlyBanner() {
        viewModelScope.launch {
            posPreferencesRepository.isSimpleProductsOnlyBannerShown.collect { isShown ->
                withContext(Dispatchers.Main) {
                    val currentState = _viewState.value
                    if (currentState is WooPosProductsViewState.Content) {
                        _viewState.value = currentState.copy(
                            bannerState = currentState.bannerState?.copy(
                                isSimpleProductsOnlyBannerShown = isShown
                            )
                        )
                    }
                }
            }
        }
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

            WooPosProductsUIEvent.SimpleProductsBannerClosed -> {
                onSimpleProductsOnlyBannerClosed()
            }
            WooPosProductsUIEvent.SimpleProductsBannerLearnMoreClicked -> TODO()
            WooPosProductsUIEvent.SimpleProductsDialogInfoIconClicked -> {

            }
        }
    }

    private fun onSimpleProductsOnlyBannerClosed() {
        viewModelScope.launch {
            posPreferencesRepository.setSimpleProductsOnlyBannerShown(true)
        }
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
        loadingMore = false,
        reloadingProducts = false,
        bannerState = WooPosProductsViewState.Content.BannerState(
            isSimpleProductsOnlyBannerShown = shouldShowProductsOnlyBanner(),
            title = R.string.woopos_banner_simple_products_only_title,
            message = R.string.woopos_banner_simple_products_only_message,
            icon = R.drawable.info,
        )
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

    private suspend fun shouldShowProductsOnlyBanner(): Boolean {
        return posPreferencesRepository.isSimpleProductsOnlyBannerShown.first()
    }
}
