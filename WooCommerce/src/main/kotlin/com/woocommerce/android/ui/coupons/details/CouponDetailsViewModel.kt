package com.woocommerce.android.ui.coupons.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class CouponDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
) : ScopedViewModel(savedState) {
    private val currencyCode by lazy {
        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    }

    val couponState = loadCoupon().asLiveData()

    @Suppress("MagicNumber")
    private fun loadCoupon(): Flow<CouponDetailsState> = flow {
        emit(
            CouponDetailsState(
                coupon = CouponUi(
                    id = 1,
                    code = "ABCDE",
                    amount = BigDecimal(25),
                    formattedDiscount = "25%",
                    affectedArticles = "Everything excl. 5 products",
                    formattedSpendingInfo = "Minimum spend of $20 \n\nMaximum spend of $200 \n",
                    isActive = true,
                    shareCodeMessage = "Apply 25% off to select products with the promo code ABCDE"
                ),
            )
        )
    }

    fun onCopyButtonClick() {
        couponState.value?.coupon?.code?.let {
            triggerEvent(CopyCodeEvent(it))
        }
        // todo handle error state
    }

    fun onShareButtonClick() {
        couponState.value?.coupon?.shareCodeMessage?.let {
            triggerEvent(ShareCodeEvent(it))
        }
        // todo handle error state
    }

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
        val isActive: Boolean,
        val shareCodeMessage: String
    )

    data class CopyCodeEvent(val couponCode: String) : MultiLiveEvent.Event()
    data class ShareCodeEvent(val shareCodeMessage: String) : MultiLiveEvent.Event()
}
