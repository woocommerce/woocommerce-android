package com.woocommerce.android.ui.coupons.selector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.coupons.CouponListHandler
import com.woocommerce.android.ui.coupons.CouponListItem
import com.woocommerce.android.ui.coupons.toUiModel
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class CouponSelectorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val couponListHandler: CouponListHandler,
    private val couponUtils: CouponUtils,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {

    companion object {
        private const val LOADING_STATE_DELAY = 100L
    }

    private val currencyCode by lazy { wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode }

    private val loadingState = MutableStateFlow(LoadingState.Idle)

    val couponSelectorState = combine(
        flow = couponListHandler.couponsFlow
            .map { coupons -> coupons.map { it.toUiModel(couponUtils, currencyCode) } },
        flow2 = loadingState.withIndex()
            .debounce {
                if (it.index != 0 && it.value == LoadingState.Idle) {
                    // When resetting to Idle, wait a bit to make sure the coupons list has been fetched from DB
                    LOADING_STATE_DELAY
                } else {
                    0L
                }
            }
            .map { it.value },
    ) { coupons, loadingState ->
        CouponSelectorState(
            loadingState = loadingState,
            coupons = coupons
        )
    }.asLiveData()

    init {
        fetchCoupons()
    }

    fun onCouponClicked(coupon: CouponListItem) {
        triggerEvent(MultiLiveEvent.Event.ExitWithResult(coupon.code))
    }

    fun onLoadMore() {
        launch {
            loadingState.value = LoadingState.Appending
            couponListHandler
                .loadMore()
                .onFailure {
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.coupon_list_loading_failed))
                }
            loadingState.value = LoadingState.Idle
        }
    }

    fun onRefresh() = launch {
        loadingState.value = LoadingState.Refreshing
        couponListHandler
            .fetchCoupons(forceRefresh = true)
            .onFailure { triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.coupon_list_loading_failed)) }
        loadingState.value = LoadingState.Idle
    }

    fun onEmptyScreenButtonClicked() {
        triggerEvent(NavigateToCouponList)
        analyticsTrackerWrapper.track(AnalyticsEvent.ORDER_GO_TO_COUPON_BUTTON_TAPPED)
    }
    private fun fetchCoupons() = launch {
        loadingState.value = LoadingState.Loading
        couponListHandler.fetchCoupons(forceRefresh = true)
            .onFailure {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.coupon_list_loading_failed))
            }
        loadingState.value = LoadingState.Idle
    }
}

data class CouponSelectorState(
    val loadingState: LoadingState = LoadingState.Idle,
    val coupons: List<CouponListItem> = emptyList(),
)

enum class LoadingState {
    Idle, Loading, Refreshing, Appending
}

object NavigateToCouponList : MultiLiveEvent.Event()
