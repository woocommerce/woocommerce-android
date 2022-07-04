package com.woocommerce.android.ui.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.Date
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class CouponListViewModel @Inject constructor(
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

    private val currencyCode by lazy {
        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    }

    private val searchQuery = savedState.getNullableStateFlow(this, null, clazz = String::class.java)
    private val loadingState = MutableStateFlow(LoadingState.Idle)

    val couponsState = combine(
        flow = couponListHandler.couponsFlow
            .map { coupons -> coupons.map { it.toUiModel() } },
        flow2 = loadingState.withIndex()
            .debounce {
                if (it.index != 0 && it.value == LoadingState.Idle) {
                    // When resetting to Idle, wait a bit to make sure the coupons list has been fetched from DB
                    LOADING_STATE_DELAY
                } else 0L
            }
            .map { it.value },
        flow3 = searchQuery
    ) { coupons, loadingState, searchQuery ->
        CouponListState(
            loadingState = loadingState,
            coupons = coupons,
            searchQuery = searchQuery
        )
    }.asLiveData()

    init {
        if (searchQuery.value == null) {
            fetchCoupons()
        }
        monitorSearchQuery()
    }

    private fun Coupon.toUiModel(): CouponListItem {
        return CouponListItem(
            id = id,
            code = code,
            summary = couponUtils.generateSummary(this, currencyCode),
            isActive = dateExpires?.after(Date()) ?: true
        )
    }

    fun onCouponClick(couponId: Long) {
        triggerEvent(NavigateToCouponDetailsEvent(couponId))
    }

    fun onLoadMore() {
        viewModelScope.launch {
            loadingState.value = LoadingState.Appending
            couponListHandler.loadMore().onFailure {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.coupon_list_loading_failed))
            }
            loadingState.value = LoadingState.Idle
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onSearchStateChanged(open: Boolean) {
        searchQuery.value = if (open) {
            analyticsTrackerWrapper.track(AnalyticsEvent.COUPONS_LIST_SEARCH_TAPPED)
            searchQuery.value.orEmpty()
        } else {
            null
        }
    }

    fun onRefresh() = launch {
        loadingState.value = LoadingState.Refreshing
        couponListHandler.fetchCoupons(forceRefresh = true)
            .onFailure {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.coupon_list_loading_failed))
            }
        loadingState.value = LoadingState.Idle
    }

    private fun fetchCoupons() = launch {
        loadingState.value = LoadingState.Loading
        couponListHandler.fetchCoupons(forceRefresh = true)
            .onFailure {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.coupon_list_loading_failed))
            }
        loadingState.value = LoadingState.Idle
    }

    private fun monitorSearchQuery() {
        viewModelScope.launch {
            searchQuery
                .withIndex()
                .filterNot {
                    // Skip initial value to avoid double fetching coupons
                    it.index == 0 && it.value == null
                }
                .map { it.value }
                .onEach {
                    loadingState.value = LoadingState.Loading
                }
                .debounce {
                    if (it.isNullOrEmpty()) 0L else AppConstants.SEARCH_TYPING_DELAY_MS
                }
                .collectLatest { query ->
                    try {
                        couponListHandler.fetchCoupons(searchQuery = query)
                            .onFailure {
                                triggerEvent(
                                    MultiLiveEvent.Event.ShowSnackbar(
                                        if (query == null) R.string.coupon_list_loading_failed
                                        else R.string.coupon_list_search_failed
                                    )
                                )
                            }
                    } finally {
                        loadingState.value = LoadingState.Idle
                    }
                }
        }
    }

    data class CouponListState(
        val loadingState: LoadingState = LoadingState.Idle,
        val searchQuery: String? = null,
        val coupons: List<CouponListItem> = emptyList()
    ) {
        val isSearchOpen = searchQuery != null
    }

    data class CouponListItem(
        val id: Long,
        val code: String? = null,
        val summary: String,
        val isActive: Boolean
    )

    enum class LoadingState {
        Idle, Loading, Refreshing, Appending
    }

    data class NavigateToCouponDetailsEvent(val couponId: Long) : MultiLiveEvent.Event()
}
