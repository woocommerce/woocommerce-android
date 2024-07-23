package com.woocommerce.android.ui.woopos.home.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosProductsViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val priceFormat: WooPosFormatPrice,
) : ViewModel() {
    private var loadMoreProductsJob: Job? = null

    private val _viewState = MutableStateFlow<WooPosProductsViewState>(WooPosProductsViewState.Loading())
    val viewState: StateFlow<WooPosProductsViewState> = _viewState
        .onEach { notifyParentAboutStatusChange(it) }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    init {
        listenToProducts()
    }

    private fun listenToProducts() {
        viewModelScope.launch {
            productsDataSource.products
                .map { products -> calculateViewState(products) }
                .collect { _viewState.value = it }
        }
        viewModelScope.launch {
            loadProducts(withPullToRefresh = false)
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
                loadProducts(withPullToRefresh = true)
            }

            WooPosProductsUIEvent.ProductsLoadingErrorRetryButtonClicked -> {
                loadProducts(withPullToRefresh = false)
            }
        }
    }

    private fun loadProducts(withPullToRefresh: Boolean) {
        viewModelScope.launch {
            updateProductsReloadingState(isReloading = withPullToRefresh)
            val result = productsDataSource.loadSimpleProducts(forceRefreshProducts = true)
            if (result.isFailure) {
                _viewState.value = WooPosProductsViewState.Error()
            } else if (withPullToRefresh) {
                updateProductsReloadingState(isReloading = false)
            }
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

    private fun notifyParentAboutStatusChange(newState: WooPosProductsViewState) {
        sendEventToParent(
            when (newState) {
                is WooPosProductsViewState.Content -> ChildToParentEvent.ProductsStatusChanged.WithCart

                is WooPosProductsViewState.Empty,
                is WooPosProductsViewState.Error,
                is WooPosProductsViewState.Loading -> ChildToParentEvent.ProductsStatusChanged.FullScreen
            }
        )
    }

    private fun onItemClicked(item: WooPosProductsListItem) {
        sendEventToParent(ChildToParentEvent.ItemClickedInProductSelector(item.id))
    }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch { fromChildToParentEventSender.sendToParent(event) }
    }
}
