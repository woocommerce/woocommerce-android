package com.woocommerce.android.ui.woopos.home.items.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.util.isNetworkError
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosProductsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogProductSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.ProductsResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIncrementalSyncReason
import com.woocommerce.android.ui.woopos.localcatalog.WooPosPerformLocalCatalogIncrementalSync
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.PullToRefreshTriggered
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.CoroutineDispatchers
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
class WooPosProductsViewModel @Inject constructor(
    private val dataSource: WooPosProductsDataSource,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val priceFormat: WooPosFormatPrice,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val resourceProvider: ResourceProvider,
    private val dispatchers: CoroutineDispatchers,
    incrementalSync: WooPosPerformLocalCatalogIncrementalSync,
) : ViewModel() {

    private var loadMoreProductsJob: Job? = null
    private var loadProductsJob: Job? = null
    private var loadMoreAfterLoadCompletes = false

    private val _viewState = MutableStateFlow<WooPosProductsViewState>(
        WooPosProductsViewState.Loading()
    )

    val viewState: StateFlow<WooPosProductsViewState> = _viewState
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    init {
        listenEventsFromParent()
        loadProducts(forceRefreshProducts = false)
        incrementalSync.execute(WooPosIncrementalSyncReason.ON_POS_PRODUCT_LIST)
    }

    private fun listenEventsFromParent() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    ParentToChildrenEvent.RefreshProductList -> loadProducts(forceRefreshProducts = true)

                    ParentToChildrenEvent.BackFromCheckoutToCartClicked,
                    is ParentToChildrenEvent.BarcodeEvent,
                    is ParentToChildrenEvent.CheckoutClicked,
                    is ParentToChildrenEvent.CouponsRemoved,
                    ParentToChildrenEvent.CouponsValidationFailed,
                    is ParentToChildrenEvent.ItemClickedInItemsList,
                    is ParentToChildrenEvent.OrderCreated,
                    is ParentToChildrenEvent.OrderSuccessfullyPaid,
                    ParentToChildrenEvent.RemoveCouponsClicked,
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery,
                    ParentToChildrenEvent.SearchEvent.Finished,
                    is ParentToChildrenEvent.SearchEvent.RecentSearchSelected,
                    ParentToChildrenEvent.SearchEvent.Started,
                    is ParentToChildrenEvent.RemoveProductsClicked,
                    is ParentToChildrenEvent.MissingVariationEvent,
                    is ParentToChildrenEvent.ProductsRemoved,
                    is ParentToChildrenEvent.SettingsEvent -> Unit
                }
            }
        }
    }

    fun onUIEvent(event: WooPosProductsUIEvent) {
        when (event) {
            is WooPosProductsUIEvent.EndOfItemsListReached -> {
                onEndOfProductsListReached()
            }

            is WooPosProductsUIEvent.ItemClicked -> {
                handleItemClick(event)
            }

            WooPosProductsUIEvent.PullToRefreshTriggered -> {
                handlePullToRefresh()
                viewModelScope.launch {
                    analyticsTracker.track(
                        PullToRefreshTriggered(
                            WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                            WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
                        )
                    )
                }
            }

            WooPosProductsUIEvent.ProductsLoadingErrorRetryButtonClicked -> loadProducts(forceRefreshProducts = false)
        }
    }

    private fun handleItemClick(event: WooPosProductsUIEvent.ItemClicked) {
        when (event.item) {
            is WooPosItemSelectionViewState.Product.Simple -> {
                onSimpleProductClicked(event.item)
            }

            is WooPosItemSelectionViewState.Product.Variable -> {
                onVariableProductClicked(event.item)
            }

            is WooPosItemSelectionViewState.Product.Variation -> error("Variation item not supported in products list")
            is WooPosItemSelectionViewState.Product.VariationSearchResult ->
                error("VariationSearchResult item not supported in products list")
            is WooPosItemSelectionViewState.Coupon -> error("Coupon item isn't supported in products list")
        }
    }

    private fun onVariableProductClicked(item: WooPosItemSelectionViewState.Product.Variable) {
        viewModelScope.launch {
            fromChildToParentEventSender.sendToParent(
                ChildToParentEvent.ItemClickedInItemsList(
                    itemData = ItemClickedData.VariableProduct(
                        id = item.id,
                        name = item.name,
                        numOfVariations = item.numOfVariations,
                        sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST,
                    ),
                    eventForTracking = null,
                )
            )
        }
    }

    private fun loadProducts(forceRefreshProducts: Boolean) {
        loadProductsJob?.cancel()
        loadMoreProductsJob?.cancel()
        loadProductsJob = viewModelScope.launch(dispatchers.io) {
            _viewState.value = WooPosProductsViewState.Loading()

            dataSource.fetchFirstPage(forceRefresh = forceRefreshProducts).collect { result ->
                when (result) {
                    is ProductsResult.Cached -> {
                        if (result.products.isNotEmpty()) {
                            _viewState.value = result.products.toContentState()
                        }
                    }

                    is ProductsResult.Remote -> {
                        _viewState.value = when {
                            result.productsResult.isSuccess -> {
                                val products = result.productsResult.getOrThrow()
                                if (products.isNotEmpty()) {
                                    val currentState = _viewState.value
                                    val paginationState =
                                        if (loadMoreProductsJob?.isActive == true && dataSource.hasMorePages) {
                                            WooPosPaginationState.Loading
                                        } else {
                                            WooPosPaginationState.None
                                        }
                                    if (currentState is WooPosProductsViewState.Content) {
                                        currentState.copy(
                                            items = products.map { it.toItemSelectionViewState() },
                                            paginationState = paginationState,
                                            pullToRefreshState = WooPosPullToRefreshState.Enabled
                                        )
                                    } else {
                                        products.toContentState(
                                            paginationState = paginationState,
                                        )
                                    }
                                } else {
                                    WooPosProductsViewState.Empty()
                                }
                            }

                            else -> WooPosProductsViewState.Error()
                        }

                        if (loadMoreAfterLoadCompletes) {
                            queueLoadMoreAfterLoadCompletes()
                        }
                    }
                }
            }
        }
    }

    private fun queueLoadMoreAfterLoadCompletes() {
        loadProductsJob?.invokeOnCompletion { throwable ->
            if (throwable == null && _viewState.value is WooPosProductsViewState.Content) {
                loadMoreAfterLoadCompletes = false
                onEndOfProductsListReached()
            }
        }
    }

    private fun buildReloadingState() =
        when (val state = viewState.value) {
            is WooPosProductsViewState.Content -> state.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
            is WooPosProductsViewState.Loading -> state.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
            is WooPosProductsViewState.Error -> state.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
            is WooPosProductsViewState.Empty -> state.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
        }

    private suspend fun List<WooPosProductModel>.toContentState(
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) = WooPosProductsViewState.Content(
        items = map { it.toItemSelectionViewState() },
        paginationState = paginationState,
        pullToRefreshState = WooPosPullToRefreshState.Enabled,
    )

    private suspend fun WooPosProductModel.toItemSelectionViewState(): WooPosItemSelectionViewState {
        return if (this.isVariable()) {
            WooPosItemSelectionViewState.Product.Variable(
                id = this.remoteId,
                name = this.name,
                price = priceFormat(this.pricing.displayPrice),
                imageUrl = this.firstImageUrl,
                numOfVariations = this.variationIds.size,
                variationIds = this.variationIds
            )
        } else {
            WooPosItemSelectionViewState.Product.Simple(
                id = this.remoteId,
                name = this.name,
                price = priceFormat(this.pricing.displayPrice),
                imageUrl = this.firstImageUrl,
            )
        }
    }

    @Suppress("ReturnCount")
    private fun onEndOfProductsListReached() {
        val currentState = _viewState.value
        if (currentState !is WooPosProductsViewState.Content) {
            return
        }

        if (loadProductsJob?.isActive == true) {
            loadMoreAfterLoadCompletes = true
            if (dataSource.hasMorePages) {
                _viewState.value = currentState.copy(paginationState = WooPosPaginationState.Loading)
            }
            return
        }

        if (loadMoreProductsJob?.isActive == true) {
            return
        }

        if (!dataSource.hasMorePages) {
            return
        }

        _viewState.value = currentState.copy(paginationState = WooPosPaginationState.Loading)

        loadMoreProductsJob?.cancel()
        loadMoreProductsJob = viewModelScope.launch {
            val result = dataSource.loadMore()
            val latestState = _viewState.value
            if (latestState !is WooPosProductsViewState.Content) return@launch
            _viewState.value = if (result.isSuccess) {
                appendNewProducts(latestState, result.getOrThrow()).also {
                    analyticsTracker.track(
                        WooPosAnalyticsEvent.Event.ItemsNextPageLoaded(
                            source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                            sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
                        )
                    )
                }
            } else {
                latestState.copy(paginationState = WooPosPaginationState.Error)
            }
        }
    }

    private suspend fun appendNewProducts(
        currentState: WooPosProductsViewState.Content,
        allProducts: List<WooPosProductModel>,
    ): WooPosProductsViewState.Content {
        val newItems = allProducts.drop(currentState.items.size)
            .map { it.toItemSelectionViewState() }
        return currentState.copy(
            items = currentState.items + newItems,
            paginationState = WooPosPaginationState.None,
        )
    }

    private fun onSimpleProductClicked(product: WooPosItemSelectionViewState.Product.Simple) {
        val itemData = ItemClickedData.Product.Simple(id = product.id)
        sendEventToParent(
            ChildToParentEvent.ItemClickedInItemsList(
                itemData = itemData,
                eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                    item = itemData,
                    source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                    sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST,
                ),
            )
        )
    }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch { fromChildToParentEventSender.sendToParent(event) }
    }

    private fun handlePullToRefresh() {
        viewModelScope.launch {
            _viewState.value = buildReloadingState()

            val result = dataSource.refreshProducts()
            result.onSuccess { syncResult ->
                when (syncResult) {
                    is PosLocalCatalogProductSyncResult -> {
                        when (syncResult.value) {
                            is PosLocalCatalogSyncResult.Success -> {
                                _viewState.value = hidePTRIndicator()
                                loadProducts(forceRefreshProducts = false)
                            }
                            is PosLocalCatalogSyncResult.Failure -> {
                                handlePTRError(isNetworkError = isNetworkError(syncResult.value))
                            }
                        }
                    }
                    is ProductsResult.Cached -> Unit // PTR is not expected to deliver cached result
                    is ProductsResult.Remote -> {
                        if (syncResult.productsResult.isSuccess) {
                            val products = syncResult.productsResult.getOrThrow()
                            _viewState.value = if (products.isNotEmpty()) {
                                products.toContentState()
                            } else {
                                WooPosProductsViewState.Empty()
                            }
                        } else {
                            handlePTRError(
                                isNetworkError = isNetworkError(syncResult.productsResult.exceptionOrNull())
                            )
                        }
                    }
                }
            }.onFailure { error ->
                handlePTRError(isNetworkError = isNetworkError(error))
            }
        }
    }

    private fun isNetworkError(error: Any?): Boolean {
        return when (error) {
            is Throwable -> error.isNetworkError()
            is PosLocalCatalogSyncResult.Failure.NetworkError -> true
            else -> false
        }
    }

    private fun handlePTRError(isNetworkError: Boolean) {
        val messageResId = if (isNetworkError) {
            R.string.woo_pos_ptr_offline_error
        } else {
            R.string.something_went_wrong_try_again
        }
        sendEventToParent(
            ChildToParentEvent.ToastMessageDisplayed(
                message = resourceProvider.getString(messageResId)
            )
        )
        _viewState.value = hidePTRIndicator()
    }

    private fun hidePTRIndicator(): WooPosProductsViewState = when (val currentState = _viewState.value) {
        is WooPosProductsViewState.Content -> currentState.copy(
            pullToRefreshState = WooPosPullToRefreshState.Enabled
        )

        is WooPosProductsViewState.Loading -> currentState.copy(
            pullToRefreshState = WooPosPullToRefreshState.Enabled
        )

        is WooPosProductsViewState.Error -> currentState.copy(
            pullToRefreshState = WooPosPullToRefreshState.Enabled
        )

        is WooPosProductsViewState.Empty -> currentState.copy(
            pullToRefreshState = WooPosPullToRefreshState.Enabled
        )
    }

    private fun WooPosProductModel.isVariable() =
        type is WooPosProductModel.WooPosProductType.Variable ||
            type is WooPosProductModel.WooPosProductType.Variation
}
