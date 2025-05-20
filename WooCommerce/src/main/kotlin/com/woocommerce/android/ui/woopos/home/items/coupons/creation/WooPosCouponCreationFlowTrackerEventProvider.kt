package com.woocommerce.android.ui.woopos.home.items.coupons.creation

import com.woocommerce.android.analytics.IAnalyticsEvent
import com.woocommerce.android.ui.coupons.tracking.CouponCreationFlowTrackerEventProvider
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import javax.inject.Inject

class WooPosCouponCreationFlowTrackerEventProvider @Inject constructor() : CouponCreationFlowTrackerEventProvider {
    override val COUPON_CREATION_SUCCESS: IAnalyticsEvent
        get() = WooPosAnalyticsEvent.Event.CouponCreationSuccess
    override val COUPON_CREATION_FAILED: IAnalyticsEvent
        get() = WooPosAnalyticsEvent.Event.CouponCreationFailed
    override val COUPON_CREATION_INITIATED: IAnalyticsEvent
        get() = WooPosAnalyticsEvent.Event.CouponCreationInitiated
    override val COUPON_UPDATE_SUCCESS: IAnalyticsEvent
        get() = error("Event unreachable in POS")
    override val COUPON_UPDATE_FAILED: IAnalyticsEvent
        get() = error("Event unreachable in POS")
    override val COUPON_UPDATE_INITIATED: IAnalyticsEvent
        get() = error("Event unreachable in POS")
    override val COUPONS_LOAD_FAILED: IAnalyticsEvent
        get() = WooPosAnalyticsEvent.Event.CouponsLoadFailed
    override val COUPONS_LOADED: IAnalyticsEvent
        get() = WooPosAnalyticsEvent.Event.CouponsLoaded
}
