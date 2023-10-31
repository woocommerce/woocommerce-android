package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Order
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class OrderDetailTracker @Inject constructor(
    private val trackerWrapper: AnalyticsTrackerWrapper
) {
    fun trackCustomFieldsTapped() {
        trackerWrapper.track(AnalyticsEvent.ORDER_VIEW_CUSTOM_FIELDS_TAPPED)
    }

    fun trackEditButtonTapped(feeLinesCount: Int, shippingLinesCount: Int) {
        trackerWrapper.track(
            AnalyticsEvent.ORDER_EDIT_BUTTON_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_HAS_MULTIPLE_FEE_LINES to (feeLinesCount > 1),
                AnalyticsTracker.KEY_HAS_MULTIPLE_SHIPPING_LINES to (shippingLinesCount > 1)
            )
        )
    }

    fun trackReceiptViewTapped(orderId: Long, orderStatus: Order.Status) {
        trackerWrapper.track(
            AnalyticsEvent.RECEIPT_VIEW_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_ORDER_ID to orderId,
                AnalyticsTracker.KEY_STATUS to orderStatus
            )
        )
    }

    fun trackAddOrderTrackingTapped(orderId: Long, orderStatus: Order.Status, trackingProvider: String) {
        trackerWrapper.track(
            AnalyticsEvent.ORDER_TRACKING_ADD,
            mapOf(
                AnalyticsTracker.KEY_ID to orderId,
                AnalyticsTracker.KEY_STATUS to orderStatus,
                AnalyticsTracker.KEY_CARRIER to trackingProvider
            )
        )
    }

    fun trackOrderStatusChanged(orderId: Long, oldStatus: String, newStatus: String) {
        trackerWrapper.track(
            AnalyticsEvent.ORDER_STATUS_CHANGE,
            mapOf(
                AnalyticsTracker.KEY_ID to orderId,
                AnalyticsTracker.KEY_FROM to oldStatus,
                AnalyticsTracker.KEY_TO to newStatus,
                AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_FLOW_EDITING
            )
        )
    }

    fun trackOrderTrackingDeleteSucceeded() {
        trackerWrapper.track(AnalyticsEvent.ORDER_TRACKING_DELETE_SUCCESS)
    }

    fun trackOrderTrackingDeleteFailed(error: WCOrderStore.OrderError) {
        trackerWrapper.track(
            AnalyticsEvent.ORDER_TRACKING_DELETE_FAILED,
            prepareErrorEventDetails(error)
        )
    }

    fun trackOrderStatusChangeSucceeded() {
        trackerWrapper.track(AnalyticsEvent.ORDER_STATUS_CHANGE_SUCCESS)
    }

    fun trackOrderStatusChangeFailed(error: WCOrderStore.OrderError) {
        trackerWrapper.track(
            AnalyticsEvent.ORDER_STATUS_CHANGE_FAILED,
            prepareErrorEventDetails(error)
        )
    }

    fun trackOrderDetailPulledToRefresh() {
        trackerWrapper.track(AnalyticsEvent.ORDER_DETAIL_PULLED_TO_REFRESH)
    }

    fun trackShippinhLabelTapped() {
        trackerWrapper.track(AnalyticsEvent.ORDER_DETAIL_CREATE_SHIPPING_LABEL_BUTTON_TAPPED)
    }

    fun trackMarkOrderAsCompleteTapped() {
        trackerWrapper.track(AnalyticsEvent.ORDER_DETAIL_FULFILL_ORDER_BUTTON_TAPPED)
    }

    fun trackViewAddonsTapped() {
        trackerWrapper.track(AnalyticsEvent.PRODUCT_ADDONS_ORDER_DETAIL_VIEW_PRODUCT_ADDONS_TAPPED)
    }

    fun trackProductsLoaded(orderId: Long, productTypes: String, hasAddons: Boolean) {
        trackerWrapper.track(
            stat = AnalyticsEvent.ORDER_PRODUCTS_LOADED,
            properties = mapOf(
                AnalyticsTracker.KEY_ID to orderId,
                AnalyticsTracker.PRODUCT_TYPES to productTypes,
                AnalyticsTracker.HAS_ADDONS to hasAddons
            )
        )
    }

    fun trackOrderDetailsSubscriptionsShown() {
        trackerWrapper.track(AnalyticsEvent.ORDER_DETAILS_SUBSCRIPTIONS_SHOWN)
    }

    fun trackOrderDetailsGiftCardShown() {
        trackerWrapper.track(AnalyticsEvent.ORDER_DETAILS_GIFT_CARD_SHOWN)
    }

    fun trackOrderEligibleForShippingLabelCreation(orderStatus: String) {
        trackerWrapper.track(
            stat = AnalyticsEvent.SHIPPING_LABEL_ORDER_IS_ELIGIBLE,
            properties = mapOf(
                "order_status" to orderStatus
            )
        )
    }

    private fun prepareErrorEventDetails(error: WCOrderStore.OrderError) = mapOf(
        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
        AnalyticsTracker.KEY_ERROR_TYPE to error.type.toString(),
        AnalyticsTracker.KEY_ERROR_DESC to error.message
    )
}
