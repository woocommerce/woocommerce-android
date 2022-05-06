package com.woocommerce.android.ui.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.Date
import javax.inject.Inject

@FlowPreview
@HiltViewModel
class CouponListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val couponListHandler: CouponListHandler,
    private val couponUtils: CouponUtils
) : ScopedViewModel(savedState) {
    private val currencyCode by lazy {
        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    }

    private val searchQuery = savedState.getNullableStateFlow(this, null, clazz = String::class.java)
    private val isLoading = MutableStateFlow(false)

    val couponsState = combine(
        couponListHandler.couponsFlow
            .map { coupons -> coupons.map { it.toUiModel() } },
        isLoading,
        searchQuery
    ) { coupons, isLoading, searchQuery ->
        CouponListState(
            isLoading = isLoading,
            coupons = coupons,
            searchQuery = searchQuery
        )
    }
        .asLiveData()

    init {
        if (searchQuery.value == null) {
            viewModelScope.launch {
                isLoading.value = true
                couponListHandler.fetchCoupons(forceRefresh = true)
                isLoading.value = false
            }
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
            couponListHandler.loadMore()
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onSearchStateChanged(open: Boolean) {
        searchQuery.value = if (open) {
            AnalyticsTracker.track(AnalyticsEvent.COUPONS_LIST_SEARCH_TAPPED)
            searchQuery.value.orEmpty()
        } else {
            null
        }
    }

    private fun monitorSearchQuery() {
        viewModelScope.launch {
            searchQuery
                .onEach {
                    isLoading.value = true
                }
                .debounce {
                    if (it.isNullOrEmpty()) 0L else AppConstants.SEARCH_TYPING_DELAY_MS
                }.collectLatest {
                    try {
                        couponListHandler.fetchCoupons(searchQuery = it)
                    } finally {
                        isLoading.value = false
                    }
                }
        }
    }

    data class CouponListState(
        val isLoading: Boolean = false,
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

    data class NavigateToCouponDetailsEvent(val couponId: Long) : MultiLiveEvent.Event()
}
