package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.analytics.IAnalyticsEvent
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTrackerEventProvider
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.PaymentFlowTrackerEvent

class WooPosPaymentsFlowTrackerEventProvider(
    private val analyticsTrackingDataKeeper: WooPosAnalyticsTrackingDataKeeper,
) : PaymentsFlowTrackerEventProvider {
    override val CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentOnboardingLearnMoreTapped

    override val CARD_PRESENT_ONBOARDING_COMPLETED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentOnboardingCompleted

    override val CARD_PRESENT_ONBOARDING_NOT_COMPLETED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentOnboardingNotCompleted

    override val CARD_PRESENT_ONBOARDING_STEP_SKIPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentOnboardingStepSkipped

    override val CARD_PRESENT_ONBOARDING_CTA_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentOnboardingCtaTapped

    override val CARD_PRESENT_ONBOARDING_CTA_FAILED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentOnboardingCtaFailed

    override val PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.PaymentsHubCashOnDeliveryToggled

    override val ENABLE_CASH_ON_DELIVERY_SUCCESS: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.EnableCashOnDeliverySuccess

    override val ENABLE_CASH_ON_DELIVERY_FAILED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.EnableCashOnDeliveryFailed

    override val DISABLE_CASH_ON_DELIVERY_SUCCESS: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.DisableCashOnDeliverySuccess

    override val DISABLE_CASH_ON_DELIVERY_FAILED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.DisableCashOnDeliveryFailed

    override val PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED_LEARN_MORE_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.PaymentsHubCashOnDeliveryToggledLearnMoreTapped

    override val CARD_PRESENT_PAYMENT_GATEWAY_SELECTED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentPaymentGatewaySelected

    override val CARD_READER_SOFTWARE_UPDATE_STARTED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderSoftwareUpdateStarted

    override val CARD_READER_SOFTWARE_UPDATE_ALERT_SHOWN: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderSoftwareUpdateAlertShown

    override val CARD_READER_SOFTWARE_UPDATE_ALERT_INSTALL_CLICKED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderSoftwareUpdateAlertInstallClicked

    override val CARD_READER_SOFTWARE_UPDATE_FAILED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderSoftwareUpdateFailed

    override val CARD_READER_SOFTWARE_UPDATE_SUCCESS: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderSoftwareUpdateSuccess

    override val CARD_READER_DISCOVERY_READER_DISCOVERED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderDiscoveryReaderDiscovered

    override val CARD_READER_DISCOVERY_FAILED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderDiscoveryFailed

    override val CARD_READER_DISCOVERY_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderDiscoveryTapped

    override val CARD_READER_AUTO_CONNECTION_STARTED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderAutoConnectionStarted

    override val CARD_READER_CONNECTION_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderConnectionTapped

    override val CARD_READER_LOCATION_SUCCESS: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderLocationSuccess

    override val CARD_READER_LOCATION_FAILURE: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderLocationFailure

    override val CARD_READER_LOCATION_MISSING_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderLocationMissingTapped

    override val CARD_READER_CONNECTION_FAILED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderConnectionFailed

    override val CARD_READER_CONNECTION_SUCCESS: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderConnectionSuccess

    override val CARD_PRESENT_COLLECT_PAYMENT_FAILED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentCollectPaymentFailed

    override val CARD_PRESENT_COLLECT_PAYMENT_SUCCESS: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentCollectPaymentSuccess.apply {
            val props = mutableMapOf<String, String>()
            analyticsTrackingDataKeeper.interactionWithCustomerStartedTimestamp?.let {
                props["milliseconds_since_customer_interaction_started"] = "${System.currentTimeMillis() - it}"
            }
            analyticsTrackingDataKeeper.orderSyncSuccessTimestamp?.let {
                props["milliseconds_since_order_sync_success"] = "${System.currentTimeMillis() - it}"
            }
            analyticsTrackingDataKeeper.readerReadyForPaymentTimestamp?.let {
                props["milliseconds_since_reader_ready_to_collect_payment"] = "${System.currentTimeMillis() - it}"
            }
            analyticsTrackingDataKeeper.cardTappedTimestamp?.let {
                props["milliseconds_since_card_tapped"] = "${System.currentTimeMillis() - it}"
            }
            props["checkout_tap_count"] = "${analyticsTrackingDataKeeper.checkoutButtonTapsCount}"
            addProperties(props)
        }

    override val CARD_PRESENT_COLLECT_INTERAC_PAYMENT_SUCCESS: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentCollectInteracPaymentSuccess

    override val CARD_PRESENT_COLLECT_INTERAC_PAYMENT_FAILED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentCollectInteracPaymentFailed

    override val RECEIPT_PRINT_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.ReceiptPrintTapped

    override val RECEIPT_EMAIL_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.ReceiptEmailTapped

    override val RECEIPT_PRINT_CANCELED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.ReceiptPrintCanceled

    override val RECEIPT_PRINT_FAILED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.ReceiptPrintFailed

    override val RECEIPT_PRINT_SUCCESS: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.ReceiptPrintSuccess

    override val RECEIPT_VIEW_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.ReceiptViewTapped

    override val RECEIPT_URL_FETCHING_FAILS: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.ReceiptUrlFetchingFails

    override val CARD_PRESENT_COLLECT_PAYMENT_CANCELLED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentCollectPaymentCancelled

    override val PAYMENTS_FLOW_ORDER_COLLECT_PAYMENT_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.PaymentsFlowOrderCollectPaymentTapped

    override val CARD_READER_DISCONNECT_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderDisconnectTapped

    override val CARD_PRESENT_COLLECT_INTERAC_REFUND_CANCELLED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentCollectInteracRefundCancelled

    override val CARD_PRESENT_CONNECTION_LEARN_MORE_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentConnectionLearnMoreTapped

    override val IN_PERSON_PAYMENTS_LEARN_MORE_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.InPersonPaymentsLearnMoreTapped

    override val CARD_PRESENT_SELECT_READER_TYPE_BUILT_IN_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentSelectReaderTypeBuiltInTapped

    override val CARD_PRESENT_SELECT_READER_TYPE_BLUETOOTH_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentSelectReaderTypeBluetoothTapped

    override val MANAGE_CARD_READERS_AUTOMATIC_DISCONNECT_BUILT_IN_READER: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.ManageCardReadersAutomaticDisconnectBuiltInReader

    override val CARD_PRESENT_TAP_TO_PAY_NOT_AVAILABLE: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentTapToPayNotAvailable

    override val CARD_READER_AUTOMATIC_DISCONNECT: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardReaderAutomaticDisconnect

    override val CARD_PRESENT_PAYMENT_FAILED_CONTACT_SUPPORT_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentPaymentFailedContactSupportTapped

    override val CARD_PRESENT_TAP_TO_PAY_PAYMENT_FAILED_ENABLE_NFC_TAPPED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.CardPresentTapToPayPaymentFailedEnableNfcTapped

    override val PAYMENTS_FLOW_FAILED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.PaymentsFlowFailed

    override val PAYMENTS_FLOW_CANCELED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.PaymentsFlowCanceled

    override val PAYMENTS_FLOW_COLLECT: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.PaymentsFlowCollect

    override val PAYMENTS_FLOW_COMPLETED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.PaymentsFlowCompleted

    override val RECEIPT_EMAIL_FAILED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.ReceiptEmailFailed

    override val PAYMENTS_ONBOARDING_SHOWN: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.PaymentsOnboardingShown

    override val PAYMENTS_ONBOARDING_DISMISSED: IAnalyticsEvent
        get() = PaymentFlowTrackerEvent.PaymentsOnboardingDismissed
}
