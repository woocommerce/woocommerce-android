package com.woocommerce.android.ui.coupons.tracking

import com.woocommerce.android.analytics.IAnalyticsEvent

@Suppress("VariableNaming")
interface CouponCreationFlowTrackerEventProvider {
    val COUPON_CREATION_SUCCESS: IAnalyticsEvent
    val COUPON_CREATION_FAILED: IAnalyticsEvent
    val COUPON_CREATION_INITIATED: IAnalyticsEvent
    val COUPON_UPDATE_SUCCESS: IAnalyticsEvent
    val COUPON_UPDATE_FAILED: IAnalyticsEvent
    val COUPON_UPDATE_INITIATED: IAnalyticsEvent
}
