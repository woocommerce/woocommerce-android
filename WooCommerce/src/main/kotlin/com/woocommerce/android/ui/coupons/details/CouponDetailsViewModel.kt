package com.woocommerce.android.ui.coupons.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
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
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedState) {
    val couponState = loadCoupon().asLiveData()

    @Suppress("MagicNumber")
    private fun loadCoupon(): Flow<CouponSummaryState> = flow {
        emit(
            CouponSummaryState(
                currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode,
                coupon = CouponUi(
                    id = 1,
                    code = "ABCDE",
                    amount = BigDecimal(25),
                    discountType = "percent",
                    includedProductsCount = 5,
                    includedCategoryCount = 4,
                    minimumAmount = "10.5",
                    maximumAmount = "100.5"
                ),
            )
        )
    }

    data class CouponSummaryState(
        val currencyCode: String? = null,
        val isLoading: Boolean = false,
        val coupon: CouponUi? = null
    )

    data class CouponUi(
        val id: Long,
        val code: String? = null,
        val amount: BigDecimal? = null,
        val dateCreated: String? = null,
        val dateCreatedGmt: String? = null,
        val dateModified: String? = null,
        val dateModifiedGmt: String? = null,
        val discountType: String? = null,
        val description: String? = null,
        val dateExpires: String? = null,
        val dateExpiresGmt: String? = null,
        val usageCount: Int? = null,
        val isForIndividualUse: Boolean? = null,
        val usageLimit: Int? = null,
        val usageLimitPerUser: Int? = null,
        val limitUsageToXItems: Int? = null,
        val isShippingFree: Boolean? = null,
        val areSaleItemsExcluded: Boolean? = null,
        val minimumAmount: String? = null,
        val maximumAmount: String? = null,
        val includedProductsCount: Int? = null,
        val excludedProductsCount: Int? = null,
        val includedCategoryCount: Int? = null,
        val excludedCategoryCount: Int? = null
    )
}
