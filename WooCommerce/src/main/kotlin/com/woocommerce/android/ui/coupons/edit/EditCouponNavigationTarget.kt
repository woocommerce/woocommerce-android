package com.woocommerce.android.ui.coupons.edit

import com.woocommerce.android.model.Coupon.CouponRestrictions
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

sealed class EditCouponNavigationTarget : Event() {
    data class OpenDescriptionEditor(val currentDescription: String?) : EditCouponNavigationTarget()
    data class OpenCouponRestrictions(
        val restrictions: CouponRestrictions,
        val currencyCode: String,
        val showLimitUsageToXItems: Boolean
    ) : EditCouponNavigationTarget()
    data class NavigateToProductSelector(val selectedProductIds: List<Long>) : EditCouponNavigationTarget()
    data class NavigateToVariationSelector(
        val productId: Long,
        val selectedVariationIds: Set<Long>
    ) : EditCouponNavigationTarget()
}
