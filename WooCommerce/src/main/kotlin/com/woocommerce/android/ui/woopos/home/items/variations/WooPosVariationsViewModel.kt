package com.woocommerce.android.ui.woopos.home.items.variations

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.PaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosItem
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosVariationsViewState
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosVariationsViewModel @Inject constructor(
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val getProductById: WooPosGetProductById,
    private val variationsDataSource: WooPosVariationsDataSource,
    private val priceFormat: WooPosFormatPrice,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {

    private val _viewState =
        MutableStateFlow<WooPosVariationsViewState>(WooPosVariationsViewState.Loading(withCart = true))
    val viewState: StateFlow<WooPosVariationsViewState> = _viewState
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    private var fetchJob: Job? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var loadMoreJob: Job? = null

    fun init(productId: Long) {
        viewModelScope.launch {
            variationsDataSource.resetState()
        }
        loadVariations(
            productId = productId,
            withPullToRefresh = false,
            withCart = true,
            forceRefresh = false
        )
    }

    private fun loadVariations(
        productId: Long,
        forceRefresh: Boolean,
        withPullToRefresh: Boolean,
        withCart: Boolean,
    ) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _viewState.value = if (withPullToRefresh) {
                buildProductsReloadingState()
            } else {
                WooPosVariationsViewState.Loading(withCart = withCart)
            }

            variationsDataSource.fetchFirstPage(productId, forceRefresh = forceRefresh).collect { result ->
                when (result) {
                    is FetchResult.Cached -> {
                        if (result.data.isNotEmpty()) {
                            updateViewStateWithVariations(result.data, productId)
                        }
                    }

                    is FetchResult.Remote -> {
                        _viewState.value = when {
                            result.result.isSuccess -> {
                                val variations = result.result.getOrThrow()
                                if (variations.isNotEmpty()) {
                                    WooPosVariationsViewState.Content(
                                        items = variations.map {
                                            WooPosItem.Variation(
                                                id = it.remoteVariationId,
                                                name = it.getNameForPOS(getProductById(productId), resourceProvider),
                                                productId = it.remoteProductId,
                                                price = if (it.price != null) {
                                                    priceFormat(it.price)
                                                } else {
                                                    "-"
                                                },
                                                imageUrl = it.image?.source
                                            )
                                        },
                                        paginationState = if (loadMoreJob?.isActive == true) {
                                            PaginationState.Loading
                                        } else {
                                            PaginationState.None
                                        }
                                    )
                                } else {
                                    WooPosVariationsViewState.Empty()
                                }
                            }

                            else -> WooPosVariationsViewState.Error()
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateViewStateWithVariations(variations: List<ProductVariation>, productId: Long) {
        if (variations.isEmpty()) {
            _viewState.value = WooPosVariationsViewState.Empty()
        } else {
            _viewState.value = WooPosVariationsViewState.Content(
                items = variations.map {
                    WooPosItem.Variation(
                        id = it.remoteVariationId,
                        name = it.getNameForPOS(getProductById(productId), resourceProvider),
                        productId = it.remoteProductId,
                        price = if (it.price != null) {
                            priceFormat(it.price)
                        } else {
                            "-"
                        },
                        imageUrl = it.image?.source
                    )
                }
            )
        }
    }

    private fun buildProductsReloadingState() =
        when (val state = viewState.value) {
            is WooPosVariationsViewState.Content -> state.copy(reloadingProductsWithPullToRefresh = true)
            is WooPosVariationsViewState.Loading -> state.copy(reloadingProductsWithPullToRefresh = true)
            is WooPosVariationsViewState.Error -> state.copy(reloadingProductsWithPullToRefresh = true)
            is WooPosVariationsViewState.Empty -> state.copy(reloadingProductsWithPullToRefresh = true)
        }

    private fun loadMore(productId: Long, numOfVariations: Int) {
        val currentState = _viewState.value
        if (currentState !is WooPosVariationsViewState.Content) {
            return
        }

        if (!variationsDataSource.canLoadMore(numOfVariations)) {
            return
        }

        _viewState.value = currentState.copy(paginationState = PaginationState.Loading)

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            val result = variationsDataSource.loadMore(productId)
            _viewState.value = if (result.isSuccess) {
                WooPosVariationsViewState.Content(
                    items = result.getOrThrow().map {
                        WooPosItem.Variation(
                            id = it.remoteVariationId,
                            name = it.getNameForPOS(getProductById(productId), resourceProvider),
                            productId = it.remoteProductId,
                            price = if (it.price != null) {
                                priceFormat(it.price)
                            } else {
                                "-"
                            },
                            imageUrl = it.image?.source
                        )
                    }
                )
            } else {
                currentState.copy(paginationState = PaginationState.Error)
            }
        }
    }

    fun onUIEvent(event: WooPosVariationsUIEvents) {
        when (event) {
            is WooPosVariationsUIEvents.EndOfItemsListReached -> {
                onEndOfVariationsListReached(event.productId, event.numOfVariations)
            }

            is WooPosVariationsUIEvents.PullToRefreshTriggered -> {
                loadVariations(event.productId, forceRefresh = true, withPullToRefresh = true, withCart = false)
            }

            is WooPosVariationsUIEvents.VariationsLoadingErrorRetryButtonClicked -> {
                loadVariations(event.productId, forceRefresh = true, withPullToRefresh = false, withCart = false)
            }

            is WooPosVariationsUIEvents.OnItemClicked -> {
                onVariationClicked(event.productId, event.variationId)
            }
        }
    }

    private fun onVariationClicked(productId: Long, variationId: Long) {
        sendEventToParent(
            ChildToParentEvent.ItemClickedInProductSelector(
                WooPosItemsViewModel.ItemClickedData.Variation(productId, variationId)
            )
        )
    }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch { fromChildToParentEventSender.sendToParent(event) }
    }

    private fun onEndOfVariationsListReached(productId: Long, numOfVariations: Int) {
        loadMore(productId, numOfVariations)
    }
}

fun ProductVariation.getNameForPOS(
    parentProduct: Product? = null,
    resourceProvider: ResourceProvider,
): String {
    return parentProduct?.variationEnabledAttributes?.joinToString(", ") { attribute ->
        val option = attributes.firstOrNull { it.name == attribute.name }
        if (option?.option != null) {
            "${attribute.name}: ${option.option}"
        } else {
            resourceProvider.getString(
                R.string.woopos_variations_any_variation,
                attribute.name
            )
        }
    } ?: attributes.joinToString(", ") { attribute ->
        if (attribute.option != null) {
            "${attribute.name}: ${attribute.option}"
        } else {
            resourceProvider.getString(
                R.string.woopos_variations_any_variation,
                attribute.name!!
            )
        }
    }
}
