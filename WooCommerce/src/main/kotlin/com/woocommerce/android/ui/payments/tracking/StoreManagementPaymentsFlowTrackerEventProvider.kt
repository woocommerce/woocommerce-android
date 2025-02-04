package com.woocommerce.android.ui.payments.tracking

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.IAnalyticsEvent

class StoreManagementPaymentsFlowTrackerEventProvider : PaymentsFlowTrackerEventProvider {
    override val CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED

    override val CARD_PRESENT_ONBOARDING_COMPLETED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_ONBOARDING_COMPLETED

    override val CARD_PRESENT_ONBOARDING_NOT_COMPLETED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_ONBOARDING_NOT_COMPLETED

    override val CARD_PRESENT_ONBOARDING_STEP_SKIPPED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_ONBOARDING_STEP_SKIPPED

    override val CARD_PRESENT_ONBOARDING_CTA_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_ONBOARDING_CTA_TAPPED

    override val CARD_PRESENT_ONBOARDING_CTA_FAILED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_ONBOARDING_CTA_FAILED

    override val PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED: IAnalyticsEvent
        get() = AnalyticsEvent.PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED

    override val ENABLE_CASH_ON_DELIVERY_SUCCESS: IAnalyticsEvent
        get() = AnalyticsEvent.ENABLE_CASH_ON_DELIVERY_SUCCESS

    override val ENABLE_CASH_ON_DELIVERY_FAILED: IAnalyticsEvent
        get() = AnalyticsEvent.ENABLE_CASH_ON_DELIVERY_FAILED

    override val DISABLE_CASH_ON_DELIVERY_SUCCESS: IAnalyticsEvent
        get() = AnalyticsEvent.DISABLE_CASH_ON_DELIVERY_SUCCESS

    override val DISABLE_CASH_ON_DELIVERY_FAILED: IAnalyticsEvent
        get() = AnalyticsEvent.DISABLE_CASH_ON_DELIVERY_FAILED

    override val PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED_LEARN_MORE_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED_LEARN_MORE_TAPPED

    override val CARD_PRESENT_PAYMENT_GATEWAY_SELECTED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_PAYMENT_GATEWAY_SELECTED

    override val CARD_READER_SOFTWARE_UPDATE_STARTED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_STARTED

    override val CARD_READER_SOFTWARE_UPDATE_ALERT_SHOWN: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_ALERT_SHOWN

    override val CARD_READER_SOFTWARE_UPDATE_ALERT_INSTALL_CLICKED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_ALERT_INSTALL_CLICKED

    override val CARD_READER_SOFTWARE_UPDATE_FAILED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_FAILED

    override val CARD_READER_SOFTWARE_UPDATE_SUCCESS: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_SUCCESS

    override val CARD_READER_DISCOVERY_READER_DISCOVERED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_DISCOVERY_READER_DISCOVERED

    override val CARD_READER_DISCOVERY_FAILED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_DISCOVERY_FAILED

    override val CARD_READER_DISCOVERY_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_DISCOVERY_TAPPED

    override val CARD_READER_AUTO_CONNECTION_STARTED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_AUTO_CONNECTION_STARTED

    override val CARD_READER_CONNECTION_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_CONNECTION_TAPPED

    override val CARD_READER_LOCATION_SUCCESS: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_LOCATION_SUCCESS

    override val CARD_READER_LOCATION_FAILURE: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_LOCATION_FAILURE

    override val CARD_READER_LOCATION_MISSING_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_LOCATION_MISSING_TAPPED

    override val CARD_READER_CONNECTION_FAILED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_CONNECTION_FAILED

    override val CARD_READER_CONNECTION_SUCCESS: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_CONNECTION_SUCCESS

    override val CARD_PRESENT_COLLECT_PAYMENT_FAILED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_FAILED

    override val CARD_PRESENT_COLLECT_PAYMENT_SUCCESS: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS

    override val CARD_PRESENT_COLLECT_INTERAC_PAYMENT_SUCCESS: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_COLLECT_INTERAC_PAYMENT_SUCCESS

    override val CARD_PRESENT_COLLECT_INTERAC_PAYMENT_FAILED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_COLLECT_INTERAC_PAYMENT_FAILED

    override val RECEIPT_PRINT_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.RECEIPT_PRINT_TAPPED

    override val RECEIPT_EMAIL_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.RECEIPT_EMAIL_TAPPED

    override val RECEIPT_PRINT_CANCELED: IAnalyticsEvent
        get() = AnalyticsEvent.RECEIPT_PRINT_CANCELED

    override val RECEIPT_PRINT_FAILED: IAnalyticsEvent
        get() = AnalyticsEvent.RECEIPT_PRINT_FAILED

    override val RECEIPT_PRINT_SUCCESS: IAnalyticsEvent
        get() = AnalyticsEvent.RECEIPT_PRINT_SUCCESS

    override val RECEIPT_VIEW_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.RECEIPT_VIEW_TAPPED

    override val RECEIPT_URL_FETCHING_FAILS: IAnalyticsEvent
        get() = AnalyticsEvent.RECEIPT_URL_FETCHING_FAILS

    override val CARD_PRESENT_COLLECT_PAYMENT_CANCELLED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED

    override val PAYMENTS_FLOW_ORDER_COLLECT_PAYMENT_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.PAYMENTS_FLOW_ORDER_COLLECT_PAYMENT_TAPPED

    override val CARD_READER_DISCONNECT_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_DISCONNECT_TAPPED

    override val CARD_PRESENT_COLLECT_INTERAC_REFUND_CANCELLED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_COLLECT_INTERAC_REFUND_CANCELLED

    override val CARD_PRESENT_CONNECTION_LEARN_MORE_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_CONNECTION_LEARN_MORE_TAPPED

    override val IN_PERSON_PAYMENTS_LEARN_MORE_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.IN_PERSON_PAYMENTS_LEARN_MORE_TAPPED

    override val CARD_PRESENT_SELECT_READER_TYPE_BUILT_IN_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_SELECT_READER_TYPE_BUILT_IN_TAPPED

    override val CARD_PRESENT_SELECT_READER_TYPE_BLUETOOTH_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_SELECT_READER_TYPE_BLUETOOTH_TAPPED

    override val MANAGE_CARD_READERS_AUTOMATIC_DISCONNECT_BUILT_IN_READER: IAnalyticsEvent
        get() = AnalyticsEvent.MANAGE_CARD_READERS_AUTOMATIC_DISCONNECT_BUILT_IN_READER

    override val CARD_PRESENT_TAP_TO_PAY_NOT_AVAILABLE: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_TAP_TO_PAY_NOT_AVAILABLE

    override val CARD_READER_AUTOMATIC_DISCONNECT: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_READER_AUTOMATIC_DISCONNECT

    override val CARD_PRESENT_PAYMENT_FAILED_CONTACT_SUPPORT_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_PAYMENT_FAILED_CONTACT_SUPPORT_TAPPED

    override val CARD_PRESENT_TAP_TO_PAY_PAYMENT_FAILED_ENABLE_NFC_TAPPED: IAnalyticsEvent
        get() = AnalyticsEvent.CARD_PRESENT_TAP_TO_PAY_PAYMENT_FAILED_ENABLE_NFC_TAPPED

    override val PAYMENTS_FLOW_FAILED: IAnalyticsEvent
        get() = AnalyticsEvent.PAYMENTS_FLOW_FAILED

    override val PAYMENTS_FLOW_CANCELED: IAnalyticsEvent
        get() = AnalyticsEvent.PAYMENTS_FLOW_CANCELED

    override val PAYMENTS_FLOW_COLLECT: IAnalyticsEvent
        get() = AnalyticsEvent.PAYMENTS_FLOW_COLLECT

    override val PAYMENTS_FLOW_COMPLETED: IAnalyticsEvent
        get() = AnalyticsEvent.PAYMENTS_FLOW_COMPLETED

    override val RECEIPT_EMAIL_FAILED: IAnalyticsEvent
        get() = AnalyticsEvent.RECEIPT_EMAIL_FAILED

    override val PAYMENTS_ONBOARDING_SHOWN: IAnalyticsEvent
        get() = AnalyticsEvent.PAYMENTS_ONBOARDING_SHOWN

    override val PAYMENTS_ONBOARDING_DISMISSED: IAnalyticsEvent
        get() = AnalyticsEvent.PAYMENTS_ONBOARDING_DISMISSED
}
