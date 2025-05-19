package com.woocommerce.android.ui.coupons.tracking

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.IAnalyticsEvent
import javax.inject.Inject

class StoreManagementCouponCreationFlowTrackerEventProvider @Inject constructor() :
    CouponCreationFlowTrackerEventProvider {
    override val COUPON_CREATION_SUCCESS: IAnalyticsEvent
        get() = AnalyticsEvent.COUPON_CREATION_SUCCESS
    override val COUPON_CREATION_FAILED: IAnalyticsEvent
        get() = AnalyticsEvent.COUPON_CREATION_FAILED
    override val COUPON_CREATION_INITIATED: IAnalyticsEvent
        get() = AnalyticsEvent.COUPON_CREATION_INITIATED
    override val COUPON_UPDATE_SUCCESS: IAnalyticsEvent
        get() = AnalyticsEvent.COUPON_UPDATE_SUCCESS
    override val COUPON_UPDATE_FAILED: IAnalyticsEvent
        get() = AnalyticsEvent.COUPON_UPDATE_FAILED
    override val COUPON_UPDATE_INITIATED: IAnalyticsEvent
        get() = AnalyticsEvent.COUPON_UPDATE_INITIATED
}
