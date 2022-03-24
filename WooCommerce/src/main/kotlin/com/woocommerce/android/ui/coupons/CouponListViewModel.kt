package com.woocommerce.android.ui.coupons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.CouponStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CouponListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val couponRepository: CouponRepository
) : ScopedViewModel(savedState) {
    val couponsState = couponRepository.couponsFlow
        .map { coupons ->
            CouponListState(
                currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode,
                isLoading = false,
                coupons = coupons
            )
        }
        .asLiveData()

    init {
        viewModelScope.launch {
            couponRepository.loadCoupons()
        }
    }

    data class CouponListState(
        val currencyCode: String? = null,
        val isLoading: Boolean = false,
        val coupons: List<CouponUi> = emptyList()
    )

    data class CouponUi(
        val id: Long,
        val code: String? = null,
        val amount: BigDecimal? = null,
        val dateCreatedGmt: Date? = null,
        val dateModifiedGmt: Date? = null,
        val discountType: String? = null,
        val description: String? = null,
        val dateExpiresGmt: Date? = null,
        val usageCount: Int? = null,
        val isForIndividualUse: Boolean? = null,
        val usageLimit: Int? = null,
        val usageLimitPerUser: Int? = null,
        val limitUsageToXItems: Int? = null,
        val isShippingFree: Boolean? = null,
        val areSaleItemsExcluded: Boolean? = null,
        val minimumAmount: BigDecimal? = null,
        val maximumAmount: BigDecimal? = null,
        val includedProductsCount: Int,
        val excludedProductsCount: Int,
        val includedCategoryCount: Int,
        val excludedCategoryCount: Int
    )
}
