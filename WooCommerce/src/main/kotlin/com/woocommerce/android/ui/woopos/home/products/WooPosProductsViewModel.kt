package com.woocommerce.android.ui.woopos.home.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosProductsViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val priceFormat: WooPosFormatPrice,
    private val preferencesRepository: WooPosPreferencesRepository,
) : ViewModel() {
    private var loadMoreProductsJob: Job? = null

    private val _viewState =
        MutableStateFlow<WooPosProductsViewState>(WooPosProductsViewState.Loading(withCart = true))
    val viewState: StateFlow<WooPosProductsViewState> = _viewState
        .onEach { notifyParentAboutStatusChange(it) }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    init {
        loadProducts(
            forceRefreshProducts = false,
            withPullToRefresh = false,
            withCart = true,
        )
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
                loadProducts(
                    forceRefreshProducts = true,
                    withPullToRefresh = true,
                    withCart = true,
                )
            }

            WooPosProductsUIEvent.ProductsLoadingErrorRetryButtonClicked -> {
                loadProducts(
                    forceRefreshProducts = false,
                    withPullToRefresh = false,
                    withCart = false,
                )
            }

            WooPosProductsUIEvent.SimpleProductsBannerClosed -> {
                onSimpleProductsOnlyBannerClosed()
            }

            WooPosProductsUIEvent.SimpleProductsBannerLearnMoreClicked -> {
                onSimpleProductsOnlyBannerLearnMoreClicked()
            }
            WooPosProductsUIEvent.SimpleProductsDialogInfoIconClicked -> {
                onSimpleProductsDialogInfoClicked()
            }
        }
    }

    private fun onSimpleProductsOnlyBannerLearnMoreClicked() {
        onSimpleProductsDialogInfoClicked()
    }

    private fun onSimpleProductsDialogInfoClicked() {
        viewModelScope.launch {
            fromChildToParentEventSender.sendToParent(ChildToParentEvent.ProductsDialogInfoIconClicked)
        }
    }

    private fun onSimpleProductsOnlyBannerClosed() {
        viewModelScope.launch {
            val currentState = _viewState.value as WooPosProductsViewState.Content
            preferencesRepository.setSimpleProductsOnlyBannerWasHiddenByUser(true)
            _viewState.value = currentState.copy(
                bannerState = currentState.bannerState.copy(
                    isBannerHiddenByUser = true
                )
            )
        }
    }

    private fun loadProducts(
        forceRefreshProducts: Boolean,
        withPullToRefresh: Boolean,
        withCart: Boolean
    ) {
        viewModelScope.launch {
            _viewState.value = if (withPullToRefresh) {
                buildProductsReloadingState()
            } else {
                WooPosProductsViewState.Loading(withCart = withCart)
            }

            productsDataSource.loadSimpleProducts(forceRefreshProducts = forceRefreshProducts).collect { result ->
                when (result) {
                    is WooPosProductsDataSource.ProductsResult.Cached -> {
                        if (result.products.isNotEmpty()) {
                            _viewState.value = result.products.toContentState()
                        }
                    }

                    is WooPosProductsDataSource.ProductsResult.Remote -> {
                        _viewState.value = when {
                            result.productsResult.isSuccess -> {
                                val products = result.productsResult.getOrThrow()
                                if (products.isNotEmpty()) {
                                    products.toContentState()
                                } else {
                                    WooPosProductsViewState.Empty()
                                }
                            }

                            else -> WooPosProductsViewState.Error()
                        }
                    }
                }
            }
        }
    }

    private fun buildProductsReloadingState() =
        when (val state = viewState.value) {
            is WooPosProductsViewState.Content -> state.copy(reloadingProductsWithPullToRefresh = true)
            is WooPosProductsViewState.Loading -> state.copy(reloadingProductsWithPullToRefresh = true)
            is WooPosProductsViewState.Error -> state.copy(reloadingProductsWithPullToRefresh = true)
            is WooPosProductsViewState.Empty -> state.copy(reloadingProductsWithPullToRefresh = true)
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
        reloadingProductsWithPullToRefresh = false,
        bannerState = WooPosProductsViewState.Content.BannerState(
            isBannerHiddenByUser = isBannerHiddenByUser(),
            title = R.string.woopos_banner_simple_products_only_title,
            message = R.string.woopos_banner_simple_products_only_message,
            icon = R.drawable.info,
        ),
    )

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
            val result = productsDataSource.loadMore()
            _viewState.value = if (result.isSuccess) {
                result.getOrThrow().toContentState()
            } else {
                WooPosProductsViewState.Error()
            }
        }
    }

    private fun notifyParentAboutStatusChange(newState: WooPosProductsViewState) {
        sendEventToParent(
            when (newState) {
                is WooPosProductsViewState.Content -> ChildToParentEvent.ProductsStatusChanged.WithCart(
                    isProductsLoading = newState.reloadingProductsWithPullToRefresh
                )

                is WooPosProductsViewState.Empty,
                is WooPosProductsViewState.Error -> ChildToParentEvent.ProductsStatusChanged.FullScreen(
                    isProductsLoading = newState.reloadingProductsWithPullToRefresh
                )
                is WooPosProductsViewState.Loading -> {
                    if (newState.withCart) {
                        ChildToParentEvent.ProductsStatusChanged.WithCart(
                            isProductsLoading = true
                        )
                    } else {
                        ChildToParentEvent.ProductsStatusChanged.FullScreen(
                            isProductsLoading = true
                        )
                    }
                }
            }
        )
    }

    private fun onItemClicked(item: WooPosProductsListItem) {
        sendEventToParent(ChildToParentEvent.ItemClickedInProductSelector(item.id))
    }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch { fromChildToParentEventSender.sendToParent(event) }
    }

    private suspend fun isBannerHiddenByUser(): Boolean {
        return preferencesRepository.isSimpleProductsOnlyBannerWasHiddenByUser.first()
    }
}
