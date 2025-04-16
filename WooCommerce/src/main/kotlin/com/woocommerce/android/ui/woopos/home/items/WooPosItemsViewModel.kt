package com.woocommerce.android.ui.woopos.home.items

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsCouponsEnabled
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsProductsSearchEnabled
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemNavigationData.VariableProductData
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen
import com.woocommerce.android.ui.woopos.home.items.providers.WooPosItemDataProvider
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ProductsPullToRefreshTriggered
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
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
import javax.inject.Named

@HiltViewModel
class WooPosItemsViewModel @Inject constructor(
    @Named("ProductProvider") private val productDataProvider: WooPosItemDataProvider,
    @Named("CouponsProvider") private val couponDataProvider: WooPosItemDataProvider,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val preferencesRepository: WooPosPreferencesRepository,
    private val navigator: WooPosItemsNavigator,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val searchHelper: WooPosItemsSearchHelper,
    private val isProductsSearchEnabled: WooPosIsProductsSearchEnabled,
    private val isCouponsEnabled: WooPosIsCouponsEnabled,
) : ViewModel() {
    private val _viewState =
        MutableStateFlow<WooPosItemsViewState>(WooPosItemsViewState.Loading(withCart = true))

    var currentDataProvider = productDataProvider

    private var collectDataJob: Job? = null

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
        viewModelScope.launch {
            productDataProvider.init()
        }
        viewModelScope.launch {
            couponDataProvider.init()
        }

        updateDataProvider(productDataProvider)

    }

    private fun updateDataProvider(dataProvider: WooPosItemDataProvider) {
        currentDataProvider = dataProvider

        collectDataJob?.cancel()
        collectDataJob = viewModelScope.launch {
            currentDataProvider.data.collect { data ->
                _viewState.value =
                    when {
                        data.error != null && data.paginationState != WooPosPaginationState.Error -> {
                            WooPosItemsViewState.Error(
                                pullToRefreshState = data.pullToRefreshState,
                            )
                        }

                        data.items.isEmpty() && data.pullToRefreshState == WooPosPullToRefreshState.Disabled && data.paginationState == WooPosPaginationState.None -> {
                            WooPosItemsViewState.Loading(
                                pullToRefreshState = data.pullToRefreshState,
                                withCart = true
                            )
                        }

                        data.items.isEmpty() && data.pullToRefreshState != WooPosPullToRefreshState.Refreshing && data.paginationState != WooPosPaginationState.Loading -> {
                            WooPosItemsViewState.Empty(pullToRefreshState = data.pullToRefreshState)
                        }

                        else -> {
                            WooPosItemsViewState.Content(
                                items = data.items,
                                paginationState = data.paginationState,
                                pullToRefreshState = data.pullToRefreshState,
                                couponsEnabled = isCouponsEnabled.invoke(),
                                bannerState = WooPosItemsViewState.Content.BannerState(
                                    isBannerHiddenByUser = isBannerHiddenByUser(),
                                    title = R.string.woopos_banner_simple_products_only_title,
                                    message = R.string.woopos_banner_simple_products_only_message,
                                    icon = R.drawable.info,
                                ),
                                search = searchHelper.getInitialSearchState(isProductsSearchEnabled())
                            )
                        }
                    }
            }
        }
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
                viewModelScope.launch {
                    currentDataProvider.fetchItems(forceRefresh = true)
                    analyticsTracker.track(ProductsPullToRefreshTriggered)
                }
            }

            WooPosItemsUIEvent.ProductsLoadingErrorRetryButtonClicked -> {
                viewModelScope.launch {
                    currentDataProvider.fetchItems(forceRefresh = false)
                }
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

            WooPosItemsUIEvent.CouponsButtonClicked -> {
                if (currentDataProvider == productDataProvider)
                    updateDataProvider(couponDataProvider)
                else
                    updateDataProvider(productDataProvider)
            }
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

            is WooPosItemSelectionViewState.Coupon -> {
                sendEventToParent(
                    ChildToParentEvent.ItemClickedInProductSelector(
                        ItemClickedData.Coupon(id = event.item.id, couponCode = event.item.name)
                    )
                )
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

    private fun onEndOfProductsListReached() {
        // todo if no more pages or not content, don't start.
        viewModelScope.launch { currentDataProvider.loadMore() }
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
        data class Coupon(override val id: Long, val couponCode: String) : ItemClickedData(id), Parcelable
    }
}
