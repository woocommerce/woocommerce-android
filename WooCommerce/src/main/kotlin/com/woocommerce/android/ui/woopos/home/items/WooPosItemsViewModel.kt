package com.woocommerce.android.ui.woopos.home.items

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsProductsSearchEnabled
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemNavigationData.VariableProductData
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewState.Content.ContentState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewState.Tab
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ProductsPullToRefreshTriggered
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
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
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class WooPosItemsViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val priceFormat: WooPosFormatPrice,
    private val preferencesRepository: WooPosPreferencesRepository,
    private val navigator: WooPosItemsNavigator,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val searchHelper: WooPosItemsSearchHelper,
    private val isProductsSearchEnabled: WooPosIsProductsSearchEnabled,
    private val tabsHelper: WooPosItemsTabsHelper,
) : ViewModel() {
    private var loadMoreProductsJob: Job? = null

    private val _viewState = MutableStateFlow<WooPosItemsViewState>(
        WooPosItemsViewState.Loading(
            tabs = tabsHelper.defaultTabs,
            withCart = true
        )
    )
    val viewState: StateFlow<WooPosItemsViewState> = _viewState
        .onEach { notifyParentAboutStatusChange(it) }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    init {
        searchHelper.initialize(
            coroutineScope = viewModelScope,
            viewStateFlow = _viewState
        )

        loadProducts(
            forceRefreshProducts = false,
            withPullToRefresh = false,
            withCart = true,
        )
    }

    fun onUIEvent(event: WooPosItemsUIEvent) {
        when (event) {
            is WooPosItemsUIEvent.EndOfItemsListReached -> {
                onEndOfProductsListReached()
            }

            is WooPosItemsUIEvent.ItemClicked -> {
                handleItemClick(event)
            }

            WooPosItemsUIEvent.PullToRefreshTriggered -> {
                loadProducts(
                    forceRefreshProducts = true,
                    withPullToRefresh = true,
                    withCart = true,
                )
                viewModelScope.launch { analyticsTracker.track(ProductsPullToRefreshTriggered) }
            }

            WooPosItemsUIEvent.ProductsLoadingErrorRetryButtonClicked -> {
                loadProducts(
                    forceRefreshProducts = false,
                    withPullToRefresh = false,
                    withCart = false,
                )
            }

            WooPosItemsUIEvent.SimpleProductsBannerClosed -> {
                onSimpleProductsOnlyBannerClosed()
            }

            WooPosItemsUIEvent.SimpleProductsBannerLearnMoreClicked -> {
                onSimpleProductsOnlyBannerLearnMoreClicked()
            }

            WooPosItemsUIEvent.SimpleProductsDialogInfoIconClicked -> {
                onSimpleProductsDialogInfoClicked()
            }

            WooPosItemsUIEvent.BackButtonClicked -> {
                navigateBackToItemListScreen()
            }

            WooPosItemsUIEvent.ClearSearchClicked -> searchHelper.onClearSearchClicked()
            WooPosItemsUIEvent.CloseSearchClicked -> searchHelper.onCloseSearchClicked()
            is WooPosItemsUIEvent.SearchChanged -> searchHelper.onSearchChanged(
                event.query,
                event.cursorPosition
            )

            WooPosItemsUIEvent.SearchAnimationComplete -> searchHelper.onAnimationComplete()

            is WooPosItemsUIEvent.OnTabClicked -> selectTab(event.tab)
        }
    }

    private fun navigateBackToItemListScreen() {
        viewModelScope.launch {
            navigator.sendNavigationEvent(
                WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen
            )
        }
    }

    private fun handleItemClick(event: WooPosItemsUIEvent.ItemClicked) {
        when (event.item) {
            is WooPosItemSelectionViewState.Product.Simple -> {
                onItemClicked(
                    ItemClickedData.Product.Simple(
                        id = event.item.id
                    )
                )
            }

            is WooPosItemSelectionViewState.Product.Variable -> {
                viewModelScope.launch {
                    navigator.sendNavigationEvent(
                        NavigateToVariationsScreen(
                            VariableProductData(
                                id = event.item.id,
                                name = event.item.name,
                                numOfVariations = event.item.numOfVariations,
                            )
                        )
                    )
                }
            }

            is WooPosItemSelectionViewState.Variation -> {
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
            val currentState = _viewState.value as WooPosItemsViewState.Content
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
                WooPosItemsViewState.Loading(
                    withCart = withCart,
                    tabs = tabsHelper.defaultTabs,
                )
            }

            productsDataSource.loadProducts(forceRefreshProducts = forceRefreshProducts).collect { result ->
                val currentState = _viewState.value
                when (result) {
                    is WooPosProductsDataSource.ProductsResult.Cached -> {
                        if (result.products.isNotEmpty()) {
                            _viewState.value = result.products.toContentState(
                                tabs = currentState.tabs
                            )
                        }
                    }

                    is WooPosProductsDataSource.ProductsResult.Remote -> {
                        _viewState.value = when {
                            result.productsResult.isSuccess -> {
                                val products = result.productsResult.getOrThrow()
                                if (products.isNotEmpty()) {
                                    val currentState = _viewState.value
                                    val paginationState = if (loadMoreProductsJob?.isActive == true) {
                                        WooPosPaginationState.Loading
                                    } else {
                                        WooPosPaginationState.None
                                    }
                                    if (currentState is WooPosItemsViewState.Content) {
                                        currentState.copy(
                                            items = products.map { it.toItemSelectionViewState() },
                                            paginationState = paginationState,
                                            pullToRefreshState = if (searchHelper.isSearchOpen()) {
                                                WooPosPullToRefreshState.Disabled
                                            } else {
                                                WooPosPullToRefreshState.Enabled
                                            },
                                        )
                                    } else {
                                        products.toContentState(
                                            tabs = currentState.tabs,
                                            paginationState = paginationState,
                                        )
                                    }
                                } else {
                                    WooPosItemsViewState.Empty(tabs = _viewState.value.tabs)
                                }
                            }

                            else -> WooPosItemsViewState.Error(tabs = _viewState.value.tabs)
                        }
                    }
                }
            }
        }
    }

    private fun buildProductsReloadingState() =
        when (val state = viewState.value) {
            is WooPosItemsViewState.Content -> state.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
            is WooPosItemsViewState.Loading -> state.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
            is WooPosItemsViewState.Error -> state.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
            is WooPosItemsViewState.Empty -> state.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
        }

    private fun selectTab(tab: Tab) {
        fun contentState(state: WooPosItemsViewState.Content) = when (state.contentType) {
            ContentState.CouponsList -> ContentState.ProductList
            ContentState.ProductList -> ContentState.CouponsList
        }

        _viewState.value = when (val state = _viewState.value) {
            is WooPosItemsViewState.Content -> {
                state.copy(
                    contentType = contentState(state),
                    tabs = tabsHelper.selectTab(state.tabs, tab)
                )
            }

            is WooPosItemsViewState.Loading -> state.copy(tabs = tabsHelper.selectTab(state.tabs, tab))

            is WooPosItemsViewState.Error -> state.copy(tabs = tabsHelper.selectTab(state.tabs, tab))

            is WooPosItemsViewState.Empty -> state.copy(tabs = tabsHelper.selectTab(state.tabs, tab))
        }
    }

    private suspend fun List<Product>.toContentState(
        tabs: List<Tab>,
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) = WooPosItemsViewState.Content(
        contentType = ContentState.ProductList,
        items = map { it.toItemSelectionViewState() },
        paginationState = paginationState,
        pullToRefreshState = WooPosPullToRefreshState.Enabled,
        bannerState = WooPosItemsViewState.Content.BannerState(
            isBannerHiddenByUser = isBannerHiddenByUser(),
            title = R.string.woopos_banner_simple_products_only_title,
            message = R.string.woopos_banner_simple_products_only_message,
            icon = R.drawable.info,
        ),
        search = searchHelper.getInitialSearchState(isProductsSearchEnabled()),
        tabs = tabs,
    )

    private suspend fun Product.toItemSelectionViewState(): WooPosItemSelectionViewState {
        return if (this.isVariable()) {
            WooPosItemSelectionViewState.Product.Variable(
                id = this.remoteId,
                name = this.name,
                price = priceFormat(this.price),
                imageUrl = this.firstImageUrl,
                numOfVariations = this.numVariations,
                variationIds = this.variationIds
            )
        } else {
            WooPosItemSelectionViewState.Product.Simple(
                id = this.remoteId,
                name = this.name,
                price = priceFormat(this.price),
                imageUrl = this.firstImageUrl,
            )
        }
    }

    private fun onEndOfProductsListReached() {
        val currentState = _viewState.value
        if (currentState !is WooPosItemsViewState.Content) {
            return
        }

        if (!productsDataSource.hasMorePages) {
            return
        }

        _viewState.value = currentState.copy(paginationState = WooPosPaginationState.Loading)

        loadMoreProductsJob?.cancel()
        loadMoreProductsJob = viewModelScope.launch {
            val result = productsDataSource.loadMore()
            _viewState.value = if (result.isSuccess) {
                result.getOrThrow().toContentState(tabs = currentState.tabs)
            } else {
                currentState.copy(paginationState = WooPosPaginationState.Error)
            }
        }
    }

    private fun notifyParentAboutStatusChange(newState: WooPosItemsViewState) {
        sendEventToParent(
            when (newState) {
                is WooPosItemsViewState.Content -> ChildToParentEvent.ProductsStatusChanged.WithCart

                is WooPosItemsViewState.Empty,
                is WooPosItemsViewState.Error -> ChildToParentEvent.ProductsStatusChanged.FullScreen

                is WooPosItemsViewState.Loading -> {
                    if (newState.withCart) {
                        ChildToParentEvent.ProductsStatusChanged.WithCart
                    } else {
                        ChildToParentEvent.ProductsStatusChanged.FullScreen
                    }
                }
            }
        )
    }

    private fun onItemClicked(itemData: ItemClickedData) {
        sendEventToParent(ChildToParentEvent.ItemClickedInProductSelector(itemData))
    }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch { fromChildToParentEventSender.sendToParent(event) }
    }

    private suspend fun isBannerHiddenByUser(): Boolean {
        return preferencesRepository.isSimpleProductsOnlyBannerWasHiddenByUser.first()
    }

    private fun Product.isVariable() =
        productType == ProductType.VARIABLE ||
            productType == ProductType.VARIATION

    @Parcelize
    sealed class ItemClickedData(open val id: Long) : Parcelable {
        @Parcelize
        sealed class Product(override val id: Long) : ItemClickedData(id), Parcelable {
            @Parcelize
            data class Simple(override val id: Long) : Product(id), Parcelable

            @Parcelize
            data class Variation(val productId: Long, override val id: Long) : Product(id), Parcelable
        }

        @Parcelize
        data class Coupon(override val id: Long) : ItemClickedData(id), Parcelable
    }
}
