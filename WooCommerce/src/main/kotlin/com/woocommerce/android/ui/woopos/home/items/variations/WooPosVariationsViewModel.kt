package com.woocommerce.android.ui.woopos.home.items.variations

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationMapper
import com.woocommerce.android.ui.woopos.common.data.getNameForPOS
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.home.items.WooPosVariationsViewState
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogVariationSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.VariationsResult
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ItemsNextPageLoaded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
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
    private val dataSource: WooPosProductsDataSource,
    private val priceFormat: WooPosFormatPrice,
    private val resourceProvider: ResourceProvider,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val mapper: WooPosVariationMapper,
) : ViewModel() {

    private val _viewState =
        MutableStateFlow<WooPosVariationsViewState>(WooPosVariationsViewState.Loading())
    val viewState: StateFlow<WooPosVariationsViewState> = _viewState
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    private var fetchJob: Job? = null
    private lateinit var sourceType: WooPosAnalyticsEventConstant.ItemsListSourceType

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var loadMoreJob: Job? = null

    fun init(
        productId: Long,
        sourceType: WooPosAnalyticsEventConstant.ItemsListSourceType
    ) {
        this.sourceType = sourceType
        viewModelScope.launch {
            dataSource.resetVariationsListHandler()
        }
        loadVariations(
            productId = productId,
            forceRefresh = false
        )
    }

    private fun loadVariations(
        productId: Long,
        forceRefresh: Boolean,
    ) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _viewState.value = WooPosVariationsViewState.Loading()

            dataSource.fetchVariationsFirstPage(productId, forceRefresh = forceRefresh).collect { result ->
                when (result) {
                    is VariationsResult.Cached -> {
                        if (result.data.isNotEmpty()) {
                            updateViewStateWithVariations(result.data, productId)
                        }
                    }

                    is VariationsResult.Remote -> {
                        _viewState.value = when {
                            result.result.isSuccess -> {
                                val variations = result.result.getOrThrow()
                                if (variations.isNotEmpty()) {
                                    createContentState(variations, productId)
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

    private suspend fun updateViewStateWithVariations(variations: List<WooPosVariation>, productId: Long) {
        if (variations.isEmpty()) {
            _viewState.value = WooPosVariationsViewState.Empty()
        } else {
            _viewState.value = createContentState(variations, productId)
        }
    }

    private fun buildReloadingState() =
        when (val state = viewState.value) {
            is WooPosVariationsViewState.Content -> state.copy(
                pullToRefreshState = WooPosPullToRefreshState.Refreshing
            )

            is WooPosVariationsViewState.Loading -> state.copy(
                pullToRefreshState = WooPosPullToRefreshState.Refreshing
            )

            is WooPosVariationsViewState.Error -> state.copy(
                pullToRefreshState = WooPosPullToRefreshState.Refreshing,
            )

            is WooPosVariationsViewState.Empty -> state.copy(
                pullToRefreshState = WooPosPullToRefreshState.Refreshing,
            )
        }

    private fun loadMore(productId: Long, numOfVariations: Int) {
        val currentState = _viewState.value
        if (currentState !is WooPosVariationsViewState.Content) {
            return
        }

        if (!dataSource.canLoadMoreVariations(numOfVariations)) {
            return
        }

        _viewState.value = currentState.copy(paginationState = WooPosPaginationState.Loading)

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            val result = dataSource.loadMoreVariations(productId)
            _viewState.value = if (result.isSuccess) {
                trackItemsNextPageLoaded()
                WooPosVariationsViewState.Content(
                    items = result.getOrThrow().map {
                        WooPosItemSelectionViewState.Product.Variation(
                            id = it.remoteVariationId,
                            name = it.getNameForPOS(
                                mapper,
                                getProductById(productId),
                                resourceProvider
                            ),
                            productId = it.remoteProductId,
                            price = priceFormat(it.price),
                            imageUrl = it.image?.source,
                        )
                    }
                )
            } else {
                currentState.copy(paginationState = WooPosPaginationState.Error)
            }
        }
    }

    private suspend fun trackItemsNextPageLoaded() {
        analyticsTracker.track(
            ItemsNextPageLoaded(
                source = WooPosAnalyticsEventConstant.ItemsListSource.VARIATION,
                sourceType = this.sourceType,
            )
        )
    }

    fun onUIEvent(event: WooPosVariationsUIEvents) {
        when (event) {
            is WooPosVariationsUIEvents.EndOfItemsListReached -> {
                onEndOfVariationsListReached(event.productId, event.numOfVariations)
            }

            is WooPosVariationsUIEvents.PullToRefreshTriggered -> {
                handlePullToRefresh(event.productId)
                viewModelScope.launch {
                    analyticsTracker.track(
                        WooPosAnalyticsEvent.Event.PullToRefreshTriggered(
                            source = WooPosAnalyticsEventConstant.ItemsListSource.VARIATION,
                            sourceType = sourceType,
                        )
                    )
                }
            }

            is WooPosVariationsUIEvents.VariationsLoadingErrorRetryButtonClicked -> {
                loadVariations(event.productId, forceRefresh = true)
            }

            is WooPosVariationsUIEvents.OnItemClicked -> {
                onVariationClicked(event.productId, event.variationId)
            }
        }
    }

    private fun onVariationClicked(productId: Long, variationId: Long) {
        val item = WooPosItemsViewModel.ItemClickedData.Product.Variation(productId, variationId)
        sendEventToParent(
            ChildToParentEvent.ItemClickedInItemsList(
                itemData = WooPosItemsViewModel.ItemClickedData.Product.Variation(productId, variationId),
                eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                    item = item,
                    source = WooPosAnalyticsEventConstant.ItemsListSource.VARIATION,
                    sourceType = this.sourceType,
                ),
            )
        )
    }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch { fromChildToParentEventSender.sendToParent(event) }
    }

    private fun handlePullToRefresh(productId: Long) {
        viewModelScope.launch {
            _viewState.value = buildReloadingState()

            val result = dataSource.refreshVariations(productId)
            result.onSuccess { syncResult ->
                when (syncResult) {
                    is PosLocalCatalogVariationSyncResult -> {
                        when (syncResult.value) {
                            is PosLocalCatalogSyncResult.Success -> {
                                _viewState.value = hidePTRIndicator()
                            }
                            is PosLocalCatalogSyncResult.Failure -> {
                                sendEventToParent(
                                    ChildToParentEvent.ToastMessageDisplayed(
                                        message = resourceProvider.getString(R.string.offline_error)
                                    )
                                )
                                _viewState.value = hidePTRIndicator()
                            }
                        }
                    }
                    is VariationsResult.Cached -> {
                        if (syncResult.data.isNotEmpty()) {
                            _viewState.value = createContentState(syncResult.data, productId)
                        }
                    }
                    is VariationsResult.Remote -> {
                        if (syncResult.result.isSuccess) {
                            val variations = syncResult.result.getOrThrow()
                            _viewState.value = if (variations.isNotEmpty()) {
                                createContentState(variations, productId)
                            } else {
                                WooPosVariationsViewState.Empty()
                            }
                        } else {
                            sendEventToParent(
                                ChildToParentEvent.ToastMessageDisplayed(
                                    message = resourceProvider.getString(R.string.offline_error)
                                )
                            )
                            _viewState.value = hidePTRIndicator()
                        }
                    }
                }
            }.onFailure { exception ->
                sendEventToParent(
                    ChildToParentEvent.ToastMessageDisplayed(
                        message = resourceProvider.getString(R.string.offline_error)
                    )
                )
                _viewState.value = hidePTRIndicator()
            }
        }
    }

    private fun hidePTRIndicator(): WooPosVariationsViewState = when (val currentState = _viewState.value) {
        is WooPosVariationsViewState.Content -> currentState.copy(
            pullToRefreshState = WooPosPullToRefreshState.Enabled
        )

        is WooPosVariationsViewState.Loading -> currentState.copy(
            pullToRefreshState = WooPosPullToRefreshState.Enabled
        )

        is WooPosVariationsViewState.Error -> currentState.copy(
            pullToRefreshState = WooPosPullToRefreshState.Enabled
        )

        is WooPosVariationsViewState.Empty -> currentState.copy(
            pullToRefreshState = WooPosPullToRefreshState.Enabled
        )
    }

    private fun onEndOfVariationsListReached(productId: Long, numOfVariations: Int) {
        loadMore(productId, numOfVariations)
    }

    private suspend fun createContentState(
        variations: List<WooPosVariation>,
        productId: Long
    ): WooPosVariationsViewState.Content {
        val parentProduct = getProductById(productId)
        return WooPosVariationsViewState.Content(
            items = variations.map { variation ->
                WooPosItemSelectionViewState.Product.Variation(
                    id = variation.remoteVariationId,
                    name = variation.getNameForPOS(
                        mapper,
                        parentProduct,
                        resourceProvider
                    ),
                    productId = variation.remoteProductId,
                    price = priceFormat(variation.price),
                    imageUrl = variation.image?.source,
                )
            },
            paginationState = if (loadMoreJob?.isActive == true) {
                WooPosPaginationState.Loading
            } else {
                WooPosPaginationState.None
            }
        )
    }
}
