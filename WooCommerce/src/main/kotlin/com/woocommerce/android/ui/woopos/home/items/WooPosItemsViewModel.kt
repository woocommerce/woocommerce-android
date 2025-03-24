package com.woocommerce.android.ui.woopos.home.items

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsCouponsEnabled
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsProductsSearchEnabled
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemNavigationData.VariableProductData
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ProductsPullToRefreshTriggered
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class WooPosItemsViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val priceFormat: WooPosFormatPrice,
    private val preferencesRepository: WooPosPreferencesRepository,
    private val navigator: WooPosItemsNavigator,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val resourceProvider: ResourceProvider,
    private val isProductsSearchEnabled: WooPosIsProductsSearchEnabled,
    private val isCouponsEnabled: WooPosIsCouponsEnabled,
) : ViewModel() {
    private var loadMoreProductsJob: Job? = null
    private var searchJob: Job? = null

    private val _viewState =
        MutableStateFlow<WooPosItemsViewState>(WooPosItemsViewState.Loading(withCart = true))
    val viewState: StateFlow<WooPosItemsViewState> = _viewState
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

            WooPosItemsUIEvent.ClearSearchClicked -> onClearSearchClicked()
            WooPosItemsUIEvent.CloseSearchClicked -> onCloseSearchClicked()
            is WooPosItemsUIEvent.SearchChanged -> onSearchChanged(event.query)
            WooPosItemsUIEvent.SearchAnimationCompleted -> onSearchAnimationCompleted()
            WooPosItemsUIEvent.CouponsButtonClicked -> {
                sendEventToParent(
                    ChildToParentEvent.ItemClickedInProductSelector(
                        // CouponsProject: Show available coupons instead
                        ItemClickedData.Coupon(id = 0, couponCode = "DummyCoupon")
                    )
                )
            }
        }
    }

    private fun onSearchAnimationCompleted() {
        val currentState = _viewState.value as? WooPosItemsViewState.Content ?: return
        val currentSearch = currentState.search

        if (currentSearch is WooPosItemsViewState.Content.SearchState.Visible) {
            val searchState = currentSearch.state
            if (searchState is WooPosSearchInputState.Open &&
                searchState.animationState == WooPosSearchInputState.Open.AnimationState.InProgress
            ) {
                _viewState.value = currentState.copy(
                    search = WooPosItemsViewState.Content.SearchState.Visible(
                        state = searchState.copy(
                            animationState = WooPosSearchInputState.Open.AnimationState.Complete
                        )
                    )
                )
            }
        }
    }

    private fun onSearchChanged(newQuery: String) {
        searchJob?.cancel()

        val currentState = _viewState.value as? WooPosItemsViewState.Content ?: return

        if (newQuery.isEmpty()) {
            _viewState.value = currentState.copy(
                search = WooPosItemsViewState.Content.SearchState.Visible(
                    state = WooPosSearchInputState.Open(
                        input = WooPosSearchInputState.Open.Input.Hint(
                            resourceProvider.getString(R.string.woopos_search_products)
                        ),
                        isLoading = false,
                        animationState = WooPosSearchInputState.Open.AnimationState.InProgress
                    )
                )
            )
            return
        }

        _viewState.value = currentState.copy(
            search = WooPosItemsViewState.Content.SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query(newQuery),
                    isLoading = false,
                    animationState = WooPosSearchInputState.Open.AnimationState.Complete
                )
            )
        )

        @Suppress("MagicNumber")
        searchJob = viewModelScope.launch {
            try {
                delay(500)

                _viewState.value = currentState.copy(
                    search = WooPosItemsViewState.Content.SearchState.Visible(
                        state = WooPosSearchInputState.Open(
                            input = WooPosSearchInputState.Open.Input.Query(newQuery),
                            isLoading = true,
                            animationState = WooPosSearchInputState.Open.AnimationState.Complete
                        )
                    )
                )

                delay(2000)

                if (!isActive) return@launch
                _viewState.value = currentState.copy(
                    search = WooPosItemsViewState.Content.SearchState.Visible(
                        state = WooPosSearchInputState.Open(
                            input = WooPosSearchInputState.Open.Input.Query(newQuery),
                            isLoading = false,
                            animationState = WooPosSearchInputState.Open.AnimationState.Complete
                        )
                    )
                )
            } catch (_: CancellationException) {
            }
        }
    }

    private fun onCloseSearchClicked() {
        val currentState = _viewState.value as? WooPosItemsViewState.Content ?: return
        _viewState.value = currentState.copy(
            search = WooPosItemsViewState.Content.SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )
        )
    }

    private fun onClearSearchClicked() {
        val currentState = _viewState.value as? WooPosItemsViewState.Content ?: return
        val currentSearch = currentState.search
        if (currentSearch is WooPosItemsViewState.Content.SearchState.Visible) {
            val currentInput = currentSearch.state
            if (currentInput is WooPosSearchInputState.Open &&
                currentInput.input is WooPosSearchInputState.Open.Input.Hint
            ) {
                return
            }
        }

        _viewState.value = currentState.copy(
            search = WooPosItemsViewState.Content.SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Hint(
                        resourceProvider.getString(R.string.woopos_search_products)
                    ),
                    isLoading = false,
                    animationState = WooPosSearchInputState.Open.AnimationState.Complete
                )
            )
        )
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
            is WooPosItem.Product.Simple -> {
                onItemClicked(
                    ItemClickedData.SimpleProduct(
                        id = event.item.id
                    )
                )
            }

            is WooPosItem.Product.Variable -> {
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

            is WooPosItem.Variation -> {
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
                WooPosItemsViewState.Loading(withCart = withCart)
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
                                    products.toContentState(
                                        paginationState = if (loadMoreProductsJob?.isActive == true) {
                                            PaginationState.Loading
                                        } else {
                                            PaginationState.None
                                        }
                                    )
                                } else {
                                    WooPosItemsViewState.Empty()
                                }
                            }

                            else -> WooPosItemsViewState.Error()
                        }
                    }
                }
            }
        }
    }

    private fun buildProductsReloadingState() =
        when (val state = viewState.value) {
            is WooPosItemsViewState.Content -> state.copy(reloadingProductsWithPullToRefresh = true)
            is WooPosItemsViewState.Loading -> state.copy(reloadingProductsWithPullToRefresh = true)
            is WooPosItemsViewState.Error -> state.copy(reloadingProductsWithPullToRefresh = true)
            is WooPosItemsViewState.Empty -> state.copy(reloadingProductsWithPullToRefresh = true)
        }

    private suspend fun List<Product>.toContentState(
        paginationState: PaginationState = PaginationState.None
    ) = WooPosItemsViewState.Content(
        items = map { product ->
            if (product.isVariable()) {
                WooPosItem.Product.Variable(
                    id = product.remoteId,
                    name = product.name,
                    price = priceFormat(product.price),
                    imageUrl = product.firstImageUrl,
                    numOfVariations = product.numVariations,
                    variationIds = product.variationIds
                )
            } else {
                WooPosItem.Product.Simple(
                    id = product.remoteId,
                    name = product.name,
                    price = priceFormat(product.price),
                    imageUrl = product.firstImageUrl,
                )
            }
        },
        paginationState = paginationState,
        reloadingProductsWithPullToRefresh = false,
        couponsEnabled = isCouponsEnabled.invoke(),
        bannerState = WooPosItemsViewState.Content.BannerState(
            isBannerHiddenByUser = isBannerHiddenByUser(),
            title = R.string.woopos_banner_simple_products_only_title,
            message = R.string.woopos_banner_simple_products_only_message,
            icon = R.drawable.info,
        ),
        search = when (isProductsSearchEnabled()) {
            true -> WooPosItemsViewState.Content.SearchState.Visible(
                state = WooPosSearchInputState.Closed
            )

            false -> WooPosItemsViewState.Content.SearchState.Hidden
        }
    )

    private fun onEndOfProductsListReached() {
        val currentState = _viewState.value
        if (currentState !is WooPosItemsViewState.Content) {
            return
        }

        if (!productsDataSource.hasMorePages) {
            return
        }

        _viewState.value = currentState.copy(paginationState = PaginationState.Loading)

        loadMoreProductsJob?.cancel()
        loadMoreProductsJob = viewModelScope.launch {
            val result = productsDataSource.loadMore()
            _viewState.value = if (result.isSuccess) {
                result.getOrThrow().toContentState()
            } else {
                currentState.copy(paginationState = PaginationState.Error)
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
        data class SimpleProduct(override val id: Long) : ItemClickedData(id)
        data class Variation(val productId: Long, override val id: Long) : ItemClickedData(id)
        data class Coupon(override val id: Long, val couponCode: String) : ItemClickedData(id)
    }
}
