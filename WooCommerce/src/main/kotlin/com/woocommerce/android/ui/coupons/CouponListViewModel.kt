package com.woocommerce.android.ui.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
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

    val couponsState = couponRepository.couponsFlow
        .map { coupons ->
            CouponListState(
                coupons = coupons.map { it.toUiModel() }
            )
        }
        .asLiveData()

    init {
        viewModelScope.launch {
            couponRepository.loadCoupons()
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
        triggerEvent(NavigateToCouponDetailsEvent(couponId))
    }

    fun onLoadMore() {
        viewModelScope.launch {
            couponRepository.loadCoupons(loadMore = true)
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

    data class NavigateToCouponDetailsEvent(val couponId: Long) : MultiLiveEvent.Event()
}
