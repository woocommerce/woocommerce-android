package com.woocommerce.android.ui.woopos.home.items

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab
import com.woocommerce.android.ui.woopos.home.items.coupons.creation.WooPosCouponCreationFacade
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsNavigationData
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SearchButtonTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class WooPosItemsViewModel @Inject constructor(
    private val searchHelper: WooPosItemsSearchHelper,
    private val tabsHelper: WooPosItemsTabsHelper,
    private val couponCreationFacade: WooPosCouponCreationFacade,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val analyticsTracker: WooPosAnalyticsTracker,
) : ViewModel() {
    private var preservedStateBeforeOpeningVariations: WooPosItemsToolbarViewState? = null
    private val _viewState = MutableStateFlow<WooPosItemsToolbarViewState>(
        WooPosItemsToolbarViewState.ProductList(
            tabs = tabsHelper.defaultTabs,
            search = searchHelper.getInitialSearchState(),
        )
    )
    val viewState: StateFlow<WooPosItemsToolbarViewState> = _viewState
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    init {
        listenUpEvents()
        searchHelper.initialize(
            coroutineScope = viewModelScope,
            viewStateFlow = _viewState
        )
    }

    fun onUIEvent(event: WooPosItemsUIEvent) {
        when (event) {
            WooPosItemsUIEvent.BackFromVariationsClicked -> {
                navigateBackFromVariations()
            }

            WooPosItemsUIEvent.ClearSearchClicked -> searchHelper.onClearSearchClicked()
            WooPosItemsUIEvent.CloseSearchClicked -> searchHelper.onCloseSearchClicked()
            is WooPosItemsUIEvent.SearchChanged -> searchHelper.onSearchChanged(
                event.query,
                event.cursorPosition
            )

            WooPosItemsUIEvent.SearchAnimationComplete -> searchHelper.onAnimationComplete()

            is WooPosItemsUIEvent.OnTabClicked -> selectTab(event.tab)
            WooPosItemsUIEvent.SearchIconClicked -> {
                searchHelper.onSearchChanged("", 0)
                trackSearchIconClicked()
            }

            is WooPosItemsUIEvent.AddCouponIconClicked -> createAndAddCoupon()
        }
    }

    private fun listenUpEvents() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    ParentToChildrenEvent.BackFromCheckoutToCartClicked,
                    is ParentToChildrenEvent.CheckoutClicked,
                    is ParentToChildrenEvent.CouponsRemoved,
                    ParentToChildrenEvent.CouponsValidationFailed,
                    is ParentToChildrenEvent.OrderCreated,
                    is ParentToChildrenEvent.OrderSuccessfullyPaid,
                    ParentToChildrenEvent.RefreshProductList,
                    ParentToChildrenEvent.RemoveCouponsClicked,
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery,
                    ParentToChildrenEvent.SearchEvent.Finished,
                    is ParentToChildrenEvent.SearchEvent.RecentSearchSelected,
                    ParentToChildrenEvent.SearchEvent.Started -> Unit

                    is ParentToChildrenEvent.ItemClickedInItemsList -> handleItemClicked(event)
                }
            }
        }
    }

    private fun handleItemClicked(event: ParentToChildrenEvent.ItemClickedInItemsList) {
        when (event.itemData) {
            is ItemClickedData.Coupon,
            is ItemClickedData.Product.Simple,
            is ItemClickedData.Product.Variation -> Unit

            is ItemClickedData.VariableProduct -> {
                searchHelper.updateLoadingState(isLoading = false)
                preservedStateBeforeOpeningVariations = _viewState.value
                _viewState.value = WooPosItemsToolbarViewState.VariationList(
                    tabs = listOf(
                        Tab.VariationTab(
                            name = event.itemData.name,
                            highlightLevel = Tab.HighlightLevel.Full
                        )
                    ),
                    variableProductData = WooPosVariationsNavigationData(
                        id = event.itemData.id,
                        numOfVariations = event.itemData.numOfVariations,
                        sourceType = event.itemData.sourceType,
                    ),
                )
            }
        }
    }

    private fun trackSearchIconClicked() {
        viewModelScope.launch {
            val event = SearchButtonTapped(
                source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
            )
            analyticsTracker.track(event)
        }
    }

    private fun navigateBackFromVariations() {
        when (_viewState.value) {
            is WooPosItemsToolbarViewState.VariationList -> {
                _viewState.value = preservedStateBeforeOpeningVariations ?: WooPosItemsToolbarViewState.ProductList(
                    tabs = tabsHelper.defaultTabs,
                    search = searchHelper.getInitialSearchState(),
                )
                preservedStateBeforeOpeningVariations = null
            }

            else -> error("Unexpected state: ${_viewState.value}")
        }
    }

    private fun selectTab(selectedTab: Tab) {
        if (_viewState.value.tabs.size == 1) return

        val state = _viewState.value

        _viewState.value = when (selectedTab) {
            is Tab.ProductTab -> WooPosItemsToolbarViewState.ProductList(
                tabs = tabsHelper.selectTab(state.tabs, selectedTab),
                search = searchHelper.getInitialSearchState(),
            ).also {
                viewModelScope.launch {
                    analyticsTracker.track(
                        WooPosAnalyticsEvent.Event.ItemsHeaderTapped(
                            WooPosAnalyticsEventConstant.ItemsHeaderType.PRODUCT
                        )
                    )
                }
            }

            is Tab.CouponTab -> WooPosItemsToolbarViewState.CouponList(
                tabs = tabsHelper.selectTab(state.tabs, selectedTab),
            ).also {
                viewModelScope.launch {
                    analyticsTracker.track(
                        WooPosAnalyticsEvent.Event.ItemsHeaderTapped(
                            WooPosAnalyticsEventConstant.ItemsHeaderType.COUPON
                        )
                    )
                }
            }

            is Tab.VariationTab -> WooPosItemsToolbarViewState.VariationList(
                tabs = tabsHelper.selectTab(state.tabs, selectedTab),
                variableProductData = (state as WooPosItemsToolbarViewState.VariationList).variableProductData,
            )
        }
    }

    private fun createAndAddCoupon() {
        viewModelScope.launch {
            analyticsTracker.track(WooPosAnalyticsEvent.Event.CouponsCreateTapped)
            val coupon = couponCreationFacade.createCoupon()
            if (coupon != null) {
                val itemData = ItemClickedData.Coupon(coupon.id, coupon.code ?: "")
                fromChildToParentEventSender.sendToParent(
                    ChildToParentEvent.ItemClickedInItemsList(
                        itemData = itemData,
                        eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                            item = itemData,
                            source = WooPosAnalyticsEventConstant.ItemsListSource.COUPON,
                            sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
                        )
                    )
                )
            }
        }
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

        data class VariableProduct(
            override val id: Long,
            val name: String,
            val numOfVariations: Int,
            val sourceType: WooPosAnalyticsEventConstant.ItemsListSourceType,
        ) : ItemClickedData(id)

        @Parcelize
        data class Coupon(override val id: Long, val couponCode: String) : ItemClickedData(id), Parcelable
    }
}
