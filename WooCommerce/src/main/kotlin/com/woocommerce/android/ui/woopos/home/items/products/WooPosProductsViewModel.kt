package com.woocommerce.android.ui.woopos.home.items.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosProductsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.localcatalog.ProductsResult
import com.woocommerce.android.ui.woopos.localcatalog.VariationsResult
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.PullToRefreshTriggered
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
class WooPosProductsViewModel @Inject constructor(
    private val dataSource: WooPosProductsDataSource,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val priceFormat: WooPosFormatPrice,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val resourceProvider: ResourceProvider,
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
        observeProductsContinuously()
        loadProducts(
            forceRefreshProducts = false,
            withPullToRefresh = false,
        )
    }

    private fun observeProductsContinuously() {
        viewModelScope.launch {
            dataSource.fetchFirstPage(forceRefresh = false).collect { result ->
                when (result) {
                    is ProductsResult.Cached -> {
                        if (result.products.isNotEmpty()) {
                            _viewState.value = result.products.toContentState()
                        }
                    }
                    is ProductsResult.Remote -> {
                        if (result.productsResult.isSuccess) {
                            val products = result.productsResult.getOrThrow()
                            _viewState.value = if (products.isNotEmpty()) {
                                products.toContentState()
                            } else {
                                WooPosProductsViewState.Empty()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun listenEventsFromParent() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    ParentToChildrenEvent.RefreshProductList -> {
                        loadProducts(
                            forceRefreshProducts = true,
                            withPullToRefresh = false,
                        )
                    }

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

            WooPosProductsUIEvent.ProductsLoadingErrorRetryButtonClicked -> {
                loadProducts(
                    forceRefreshProducts = false,
                    withPullToRefresh = false,
                )
            }
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

    private fun loadProducts(
        forceRefreshProducts: Boolean,
        withPullToRefresh: Boolean,
    ) {
        loadProductsJob?.cancel()
        loadMoreProductsJob?.cancel()
        loadProductsJob = viewModelScope.launch {
            _viewState.value = if (withPullToRefresh) {
                buildReloadingState()
            } else {
                WooPosProductsViewState.Loading()
            }

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
            _viewState.value = if (result.isSuccess) {
                result.getOrThrow().toContentState().also {
                    analyticsTracker.track(
                        WooPosAnalyticsEvent.Event.ItemsNextPageLoaded(
                            source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                            sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
                        )
                    )
                }
            } else {
                currentState.copy(paginationState = WooPosPaginationState.Error)
            }
        }
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
            result.onSuccess { posSyncResult ->
                when (posSyncResult) {
                    is com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult.Success -> {
                        _viewState.value = hidePTRIndicator()
                    }
                    is com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult.Failure -> {
                        sendEventToParent(
                            ChildToParentEvent.ToastMessageDisplayed(
                                message = resourceProvider.getString(R.string.something_went_wrong_try_again)
                            )
                        )
                        _viewState.value = hidePTRIndicator()
                    }
                    is ProductsResult.Cached -> {
                        if (posSyncResult.products.isNotEmpty()) {
                            _viewState.value = posSyncResult.products.toContentState()
                        }
                    }
                    is ProductsResult.Remote -> {
                        if (posSyncResult.productsResult.isSuccess) {
                            val products = posSyncResult.productsResult.getOrThrow()
                            _viewState.value = if (products.isNotEmpty()) {
                                products.toContentState()
                            } else {
                                WooPosProductsViewState.Empty()
                            }
                        } else {
                            sendEventToParent(
                                ChildToParentEvent.ToastMessageDisplayed(
                                    message = resourceProvider.getString(
                                        R.string.something_went_wrong_try_again
                                    )
                                )
                            )
                            _viewState.value = hidePTRIndicator()
                        }
                    }
                    is VariationsResult -> {
                        error("Unexpected variations result in products refresh")
                    }
                }
            }.onFailure { exception ->
                sendEventToParent(
                    ChildToParentEvent.ToastMessageDisplayed(
                        message = resourceProvider.getString(R.string.something_went_wrong_try_again)
                    )
                )
                _viewState.value = hidePTRIndicator()
            }
        }
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
        type == WooPosProductModel.WooPosProductType.VARIABLE ||
            type == WooPosProductModel.WooPosProductType.VARIATION
}
