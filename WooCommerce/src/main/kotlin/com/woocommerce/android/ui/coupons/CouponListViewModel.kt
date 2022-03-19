package com.woocommerce.android.ui.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class CouponListViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    val couponsState = loadCoupons().asLiveData()

    @Suppress("MagicNumber", "UnusedPrivateMember")
    private fun loadCoupons(): Flow<CouponsState> = flow {
        emit(
            CouponsState(
                coupons = listOf(
                    CouponUi(
                        id = 1,
                        code = "ABCDE",
                        amount = BigDecimal(25),
                        discountType = "percent",
                        includedProductsCount = 5,
                        includedCategoryCount = 4
                    ),

                    CouponUi(
                        id = 2,
                        code = "10off",
                        amount = BigDecimal(10),
                        discountType = "fixed_cart"
                    ),

                    CouponUi(
                        id = 3,
                        code = "BlackFriday",
                        amount = BigDecimal(5),
                        discountType = "fixed_product"
                    ),

                )
            )
        )
    }
    data class CouponsState(
        val isLoading: Boolean = false,
        val isFeedbackFormDismissed: Boolean = false,
        val coupons: List<CouponUi> = emptyList()
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
