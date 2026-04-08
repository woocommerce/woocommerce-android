package com.woocommerce.android.ui.woopos.home.items

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.SearchState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab
import com.woocommerce.android.ui.woopos.home.items.coupons.creation.WooPosCouponCreationFacade
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsNavigationData
import com.woocommerce.android.ui.woopos.localcatalog.DateTimeProvider
import com.woocommerce.android.ui.woopos.localcatalog.WooPosFullSyncRequirement
import com.woocommerce.android.ui.woopos.localcatalog.WooPosFullSyncStatusChecker
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIsWooCommerceVersionSunsetWarningRequired
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SearchButtonTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class WooPosItemsViewModel @Inject constructor(
    private val searchHelper: WooPosItemsSearchHelper,
    private val tabsHelper: WooPosItemsTabsHelper,
    private val couponCreationFacade: WooPosCouponCreationFacade,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val preferencesRepository: WooPosPreferencesRepository,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val syncStatusChecker: WooPosFullSyncStatusChecker,
    private val dateTimeProvider: DateTimeProvider,
    private val isWooCommerceVersionSunsetWarningRequired: WooPosIsWooCommerceVersionSunsetWarningRequired,
) : ViewModel() {
    private var preservedStateBeforeOpeningVariations: WooPosItemsToolbarViewState? = null
    private val _viewState = MutableStateFlow<WooPosItemsToolbarViewState>(initialState())
    val viewState: StateFlow<WooPosItemsToolbarViewState> = _viewState
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    private val _bannerState = MutableStateFlow<WooPosItemsBannerState>(WooPosItemsBannerState.Hidden)
    val bannerState: StateFlow<WooPosItemsBannerState> = _bannerState

    init {
        listenUpEvents()
        searchHelper.initialize(
            coroutineScope = viewModelScope,
            viewStateFlow = _viewState
        )

        viewModelScope.launch {
            preferencesRepository.setWasOpenedOnce(true)
        }

        refreshBannerState()
    }

    private fun refreshBannerState() {
        viewModelScope.launch {
            val requirement = syncStatusChecker.checkSyncRequirement()
            val isSyncOverdue = requirement is WooPosFullSyncRequirement.NonBlockingRequired &&
                requirement.isOverdue

            if (isSyncOverdue) {
                trackStaleWarningShown((requirement as WooPosFullSyncRequirement.NonBlockingRequired).lastSyncTimestamp)
                _bannerState.value = WooPosItemsBannerState.SyncOverdue
                return@launch
            }

            val isWcVersionSunsetRequired = isWooCommerceVersionSunsetWarningRequired()
            if (isWcVersionSunsetRequired) {
                analyticsTracker.track(WooPosAnalyticsEvent.Event.WooCommerceVersionSunsetWarningShown)
                _bannerState.value = WooPosItemsBannerState.WooCommerceVersionSunset
                return@launch
            }

            _bannerState.value = WooPosItemsBannerState.Hidden
        }
    }

    private suspend fun trackStaleWarningShown(lastSyncTimestamp: Long) {
        val currentTime = dateTimeProvider.now()
        val timeDifferenceMs = currentTime - lastSyncTimestamp
        val hoursSinceLastSync = timeDifferenceMs.milliseconds.inWholeHours.toInt()

        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.LocalCatalogStaleWarningShown(hoursSinceLastSync)
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

            is WooPosItemsUIEvent.OnTabClicked -> selectTab(event.tab)
            WooPosItemsUIEvent.SearchIconClicked -> {
                searchHelper.onSearchChanged("", 0)
                trackSearchIconClicked()
            }

            is WooPosItemsUIEvent.AddCouponIconClicked -> createAndAddCoupon()
            WooPosItemsUIEvent.SyncOverdueBannerDismissed -> {
                _bannerState.value = WooPosItemsBannerState.Hidden
                viewModelScope.launch {
                    analyticsTracker.track(
                        WooPosAnalyticsEvent.Event.LocalCatalogStaleWarningDismissed
                    )
                }
            }

            WooPosItemsUIEvent.WooCommerceVersionSunsetBannerDismissed -> {
                _bannerState.value = WooPosItemsBannerState.Hidden
                viewModelScope.launch {
                    preferencesRepository.setWooVersionSunsetBannerDismissalTimestamp(dateTimeProvider.now())
                    analyticsTracker.track(
                        WooPosAnalyticsEvent.Event.WooCommerceVersionSunsetWarningDismissed
                    )
                }
            }
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
                    ParentToChildrenEvent.RemoveCouponsClicked,
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery,
                    ParentToChildrenEvent.SearchEvent.Finished,
                    is ParentToChildrenEvent.SearchEvent.RecentSearchSelected,
                    ParentToChildrenEvent.SearchEvent.Started,
                    is ParentToChildrenEvent.BarcodeEvent,
                    is ParentToChildrenEvent.MissingVariationEvent,
                    is ParentToChildrenEvent.RemoveProductsClicked,
                    is ParentToChildrenEvent.ProductsRemoved -> Unit

                    ParentToChildrenEvent.RefreshProductList,
                    is ParentToChildrenEvent.SettingsEvent.RetrySyncRequested -> refreshBannerState()

                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> _viewState.value = initialState()

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
                        Tab.Variations(
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
            val source = when (_viewState.value) {
                is WooPosItemsToolbarViewState.ProductList -> WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT
                is WooPosItemsToolbarViewState.CouponList -> WooPosAnalyticsEventConstant.ItemsListSource.COUPON
                is WooPosItemsToolbarViewState.VariationList -> WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT
            }
            val event = SearchButtonTapped(source = source)
            analyticsTracker.track(event)
        }
    }

    private fun navigateBackFromVariations() {
        when (_viewState.value) {
            is WooPosItemsToolbarViewState.VariationList -> {
                _viewState.value = preservedStateBeforeOpeningVariations ?: initialState()
                preservedStateBeforeOpeningVariations = null
            }

            else -> error("Unexpected state: ${_viewState.value}")
        }
    }

    private fun selectTab(selectedTab: Tab) {
        if (_viewState.value.tabs.size == 1) return

        val state = _viewState.value

        searchHelper.updateLoadingState(isLoading = false)

        _viewState.value = when (selectedTab) {
            is Tab.Products -> WooPosItemsToolbarViewState.ProductList(
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

            is Tab.Coupons -> WooPosItemsToolbarViewState.CouponList(
                tabs = tabsHelper.selectTab(state.tabs, selectedTab),
                search = SearchState.Visible(WooPosSearchInputState.Closed)
            ).also {
                viewModelScope.launch {
                    analyticsTracker.track(
                        WooPosAnalyticsEvent.Event.ItemsHeaderTapped(
                            WooPosAnalyticsEventConstant.ItemsHeaderType.COUPON
                        )
                    )
                }
            }

            is Tab.Variations -> WooPosItemsToolbarViewState.VariationList(
                tabs = tabsHelper.selectTab(state.tabs, selectedTab),
                variableProductData = (state as WooPosItemsToolbarViewState.VariationList).variableProductData,
            )
        }
    }

    private fun initialState(): WooPosItemsToolbarViewState.ProductList = WooPosItemsToolbarViewState.ProductList(
        tabs = tabsHelper.defaultTabs,
        search = searchHelper.getInitialSearchState(),
    )

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
