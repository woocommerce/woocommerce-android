package com.woocommerce.android.ui.woopos.home.items.variations

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationMapper
import com.woocommerce.android.ui.woopos.common.data.getNameForPOS
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.home.items.WooPosVariationsViewState
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncRepository
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
    private val variationsDataSource: WooPosVariationsDataSource,
    private val variationsInDbDataSource: WooPosVariationsInDbDataSource,
    private val priceFormat: WooPosFormatPrice,
    private val resourceProvider: ResourceProvider,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
    private val mapper: WooPosVariationMapper,
    private val localCatalogSyncRepository: WooPosLocalCatalogSyncRepository,
    private val selectedSite: SelectedSite,
) : ViewModel() {

    private val currentDataSource: WooPosVariationsDataSourceInterface = when (wooPosLocalCatalogM1Enabled()) {
        true -> variationsInDbDataSource
        false -> variationsDataSource
    }

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
            currentDataSource.resetState()
        }
        loadVariations(
            productId = productId,
            withPullToRefresh = false,
            forceRefresh = false
        )
    }

    private fun loadVariations(
        productId: Long,
        forceRefresh: Boolean,
        withPullToRefresh: Boolean,
    ) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _viewState.value = if (withPullToRefresh) {
                buildProductsReloadingState()
            } else {
                WooPosVariationsViewState.Loading()
            }

            currentDataSource.fetchFirstPage(productId, forceRefresh = forceRefresh).collect { result ->
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
                                            WooPosItemSelectionViewState.Product.Variation(
                                                id = it.remoteVariationId,
                                                name = it.getNameForPOS(
                                                    mapper,
                                                    getProductById(productId),
                                                    resourceProvider
                                                ),
                                                productId = it.remoteProductId,
                                                price = priceFormat(it.price),
                                                imageUrl = it.image?.source
                                            )
                                        },
                                        paginationState = if (loadMoreJob?.isActive == true) {
                                            WooPosPaginationState.Loading
                                        } else {
                                            WooPosPaginationState.None
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

    private suspend fun updateViewStateWithVariations(variations: List<WooPosVariation>, productId: Long) {
        if (variations.isEmpty()) {
            _viewState.value = WooPosVariationsViewState.Empty()
        } else {
            _viewState.value = WooPosVariationsViewState.Content(
                items = variations.map {
                    WooPosItemSelectionViewState.Product.Variation(
                        id = it.remoteVariationId,
                        name = it.getNameForPOS(
                            mapper,
                            getProductById(productId),
                            resourceProvider
                        ),
                        productId = it.remoteProductId,
                        price = priceFormat(it.price),
                        imageUrl = it.image?.source
                    )
                }
            )
        }
    }

    private fun buildProductsReloadingState() =
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

        if (!currentDataSource.canLoadMore(numOfVariations)) {
            return
        }

        _viewState.value = currentState.copy(paginationState = WooPosPaginationState.Loading)

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            val result = currentDataSource.loadMore(productId)
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
                            imageUrl = it.image?.source
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
                when {
                    wooPosLocalCatalogM1Enabled() -> {
                        performIncrementalSync()
                        viewModelScope.launch {
                            analyticsTracker.track(
                                WooPosAnalyticsEvent.Event.PullToRefreshTriggered(
                                    source = WooPosAnalyticsEventConstant.ItemsListSource.VARIATION,
                                    sourceType = sourceType,
                                )
                            )
                        }
                    }
                    else -> {
                        loadVariations(event.productId, forceRefresh = true, withPullToRefresh = true)
                        viewModelScope.launch {
                            analyticsTracker.track(
                                WooPosAnalyticsEvent.Event.PullToRefreshTriggered(
                                    source = WooPosAnalyticsEventConstant.ItemsListSource.VARIATION,
                                    sourceType = sourceType,
                                )
                            )
                        }
                    }
                }
            }

            is WooPosVariationsUIEvents.VariationsLoadingErrorRetryButtonClicked -> {
                loadVariations(event.productId, forceRefresh = true, withPullToRefresh = false)
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

    private fun performIncrementalSync() {
        _viewState.value = buildProductsReloadingState()

        viewModelScope.launch {
            selectedSite.getOrNull()?.let { site ->
                val syncResult = localCatalogSyncRepository.syncLocalCatalogIncremental(site)
                _viewState.value = getViewStateForSyncResult(syncResult)
            }
        }
    }

    private fun getViewStateForSyncResult(syncResult: PosLocalCatalogSyncResult): WooPosVariationsViewState = when (syncResult) {
        is PosLocalCatalogSyncResult.Success -> {
            hidePTRIndicator()
        }

        is PosLocalCatalogSyncResult.Failure -> {
            sendEventToParent(
                ChildToParentEvent.ToastMessageDisplayed(
                    message = resourceProvider.getString(R.string.offline_error)
                )
            )
            hidePTRIndicator()
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
}
