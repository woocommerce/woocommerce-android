package com.woocommerce.android.ui.coupons.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class CouponDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    couponDetailsRepository: CouponDetailsRepository
) : ScopedViewModel(savedState) {
    private val navArgs by savedState.navArgs<CouponDetailsFragmentArgs>()
    private val currencyCode by lazy {
        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    }

    private val couponDetails = couponDetailsRepository.loadCoupon(navArgs.couponId)

    private val couponPerformance = flowOf(Unit)

    val couponState = combine(couponDetails, couponPerformance) { details, _ ->
        CouponDetailsState(coupon = details)
    }.catch {
        // TODO trigger an error Snackbar and navigate up
    }.asLiveData()

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
