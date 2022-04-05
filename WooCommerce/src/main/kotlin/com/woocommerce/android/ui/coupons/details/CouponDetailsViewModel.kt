package com.woocommerce.android.ui.coupons.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class CouponDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val couponDetailsRepository: CouponDetailsRepository
) : ScopedViewModel(savedState) {
    private val navArgs by savedState.navArgs<CouponDetailsFragmentArgs>()
    private val currencyCode by lazy {
        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    }

    val couponState = loadCoupon().asLiveData()

    @Suppress("MagicNumber")
    private fun loadCoupon(): Flow<CouponDetailsState> = couponDetailsRepository.loadCoupon(navArgs.couponId)
        .map { CouponDetailsState(coupon = it) }

    data class CouponDetailsState(
        val isLoading: Boolean = false,
        val coupon: CouponUi? = null
    )

    data class CouponUi(
        val id: Long,
        val code: String? = null,
        val amount: BigDecimal? = null,
        val formattedDiscount: String,
        val affectedArticles: String,
        val formattedSpendingInfo: String,
        val isActive: Boolean
    )
}
