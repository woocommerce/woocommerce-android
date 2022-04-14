package com.woocommerce.android.ui.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CouponListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val couponRepository: CouponRepository,
    private val couponUtils: CouponUtils
) : ScopedViewModel(savedState) {
    private val currencyCode by lazy {
        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    }

    private var searchJob: Job? = null

    private val isLoading = MutableStateFlow(false)

    val couponsState = couponRepository.couponsFlow
        .map { coupons -> coupons.map { it.toUiModel() } }
        .combine(isLoading) { coupons, isLoading ->
            CouponListState(
                isLoading = isLoading,
                coupons = coupons
            )
        }
        .asLiveData()

    init {
        viewModelScope.launch {
            isLoading.value = true
            couponRepository.fetchCoupons(forceRefresh = true)
            isLoading.value = false
        }
    }

    private fun Coupon.toUiModel(): CouponListItem {
        return CouponListItem(
            id = id,
            code = code,
            summary = couponUtils.generateSummary(this, currencyCode),
            isActive = dateExpiresGmt?.after(Date()) ?: true
        )
    }

    fun onCouponClick(couponId: Long) {
        triggerEvent(CouponListEvent.NavigateToCouponDetailsEvent(couponId))
    }

    fun onLoadMore() {
        viewModelScope.launch {
            couponRepository.loadMore()
        }
    }

    fun onSearchQueryChanged(query: String?) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (!query.isNullOrEmpty()) {
                delay(AppConstants.SEARCH_TYPING_DELAY_MS)
            }
            isLoading.value = true
            couponRepository.fetchCoupons(query)
            isLoading.value = false
        }
    }

    data class CouponListState(
        val isLoading: Boolean = false,
        val coupons: List<CouponListItem> = emptyList()
    )

    data class CouponListItem(
        val id: Long,
        val code: String? = null,
        val summary: String,
        val isActive: Boolean
    )

    sealed class CouponListEvent : MultiLiveEvent.Event() {
        data class NavigateToCouponDetailsEvent(val couponId: Long) : MultiLiveEvent.Event()
    }
}
