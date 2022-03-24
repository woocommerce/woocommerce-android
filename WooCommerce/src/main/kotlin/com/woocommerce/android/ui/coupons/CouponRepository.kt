package com.woocommerce.android.ui.coupons

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.persistence.entity.CouponDataModel
import org.wordpress.android.fluxc.store.CouponStore
import org.wordpress.android.util.DateTimeUtils
import javax.inject.Inject

class CouponRepository @Inject constructor(
    private val store: CouponStore,
    private val site: SelectedSite
) {
    val couponsFlow by lazy {
        store.observeCoupons(site.get()).map {
            it.map { data ->
                data.toAppModel()
            }
        }
    }

    suspend fun loadCoupons() {
        store.fetchCoupons(site.get())
    }

    fun CouponDataModel.toAppModel(): CouponListViewModel.CouponUi =
        CouponListViewModel.CouponUi(
            id = couponEntity.id,
            code = couponEntity.code,
            amount = couponEntity.amount?.toBigDecimalOrNull(),
            dateCreatedGmt = couponEntity.dateCreatedGmt?.let { DateTimeUtils.dateUTCFromIso8601(it) },
            dateModifiedGmt = couponEntity.dateModifiedGmt?.let { DateTimeUtils.dateUTCFromIso8601(it) },
            discountType = couponEntity.discountType,
            description = couponEntity.description,
            dateExpiresGmt = couponEntity.dateExpiresGmt?.let { DateTimeUtils.dateUTCFromIso8601(it) },
            usageCount = couponEntity.usageCount,
            isForIndividualUse = couponEntity.isForIndividualUse,
            usageLimit = couponEntity.usageLimit,
            usageLimitPerUser = couponEntity.usageLimitPerUser,
            limitUsageToXItems = couponEntity.limitUsageToXItems,
            isShippingFree = couponEntity.isShippingFree,
            areSaleItemsExcluded = couponEntity.areSaleItemsExcluded,
            minimumAmount = couponEntity.minimumAmount?.toBigDecimalOrNull(),
            maximumAmount = couponEntity.maximumAmount?.toBigDecimalOrNull(),
            includedCategoryCount = categories.size,
            excludedCategoryCount = excludedCategories.size,
            includedProductsCount = products.size,
            excludedProductsCount = products.size
        )
}
