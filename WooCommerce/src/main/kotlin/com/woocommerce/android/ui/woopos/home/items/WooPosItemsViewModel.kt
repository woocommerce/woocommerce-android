package com.woocommerce.android.ui.woopos.home.items

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsProductsSearchEnabled
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewState.Tab
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
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
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val preferencesRepository: WooPosPreferencesRepository,
    private val navigator: WooPosItemsNavigator,
    private val searchHelper: WooPosItemsSearchHelper,
    private val isProductsSearchEnabled: WooPosIsProductsSearchEnabled,
    private val tabsHelper: WooPosItemsTabsHelper,
) : ViewModel() {
    private val _viewState = MutableStateFlow<WooPosItemsViewState>(
        WooPosItemsViewState.ProductList(
            tabs = tabsHelper.defaultTabs,
            search = searchHelper.getInitialSearchState(isProductsSearchEnabled()),
            banner =  WooPosItemsViewState.BannerState.Hidden, // TODO Fix as part of the move to `More` menu.
        )
    )
    val viewState: StateFlow<WooPosItemsViewState> = _viewState
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
    }

    fun onUIEvent(event: WooPosItemsUIEvent) {
        when (event) {
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
            val currentState = _viewState.value as WooPosItemsViewState.ProductList
            preferencesRepository.setSimpleProductsOnlyBannerWasHiddenByUser(true)
            _viewState.value = currentState.copy(banner = WooPosItemsViewState.BannerState.Hidden)
        }
    }

    private fun selectTab(selectedTab: Tab) {
        if (_viewState.value.tabs.size == 1) return

        val state = _viewState.value

        _viewState.value = when (selectedTab.stringId) {
            R.string.woopos_products_screen_title -> WooPosItemsViewState.ProductList(
                tabs = tabsHelper.selectTab(state.tabs, selectedTab),
                search = searchHelper.getInitialSearchState(isProductsSearchEnabled()),
                banner =  WooPosItemsViewState.BannerState.Hidden, // TODO Fix as part of the move to `More` menu.
            )
            R.string.woopos_coupons_screen_title -> WooPosItemsViewState.CouponList(
                tabs = tabsHelper.selectTab(state.tabs, selectedTab),
            )
            else -> error("Invalid tab $selectedTab")
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

        @Parcelize
        data class Coupon(override val id: Long) : ItemClickedData(id), Parcelable
    }
}
