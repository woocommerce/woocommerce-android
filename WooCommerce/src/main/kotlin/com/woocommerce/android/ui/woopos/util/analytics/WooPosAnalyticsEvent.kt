package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.analytics.IAnalyticsEvent
import kotlin.reflect.KClass

sealed class WooPosAnalyticsEvent : IAnalyticsEvent {
    override val siteless: Boolean = false
    override val isPosEvent: Boolean = true

    private val _properties: MutableMap<String, String> = mutableMapOf()
    val properties: Map<String, String> get() = _properties.toMap()

    fun addProperties(additionalProperties: Map<String, String>) {
        _properties.putAll(additionalProperties)
    }

    sealed class Error : WooPosAnalyticsEvent() {
        abstract val errorContext: KClass<out Any>
        abstract val errorType: String?
        abstract val errorDescription: String?

        data class OrderCreationError(
            override val errorContext: KClass<out Any>,
            override val errorType: String?,
            override val errorDescription: String?,
        ) : Error() {
            override val name: String = "order_creation_failed"
        }
    }

    sealed class Event : WooPosAnalyticsEvent() {
        data object ItemAddedToCart : Event() {
            override val name: String = "item_added_to_cart"
        }
        data object OrderCreationSuccess : Event() {
            override val name: String = "order_creation_success"
        }
        data object GetSupportTapped : Event() {
            override val name: String = "get_support_tapped"
        }
        data object ViewDocsTapped : Event() {
            override val name: String = "view_docs_tapped"
        }
        data object ExitTapped : Event() {
            override val name: String = "exit_menu_item_tapped"
        }
    }

    sealed class PaymentFlowTrackerEvent : WooPosAnalyticsEvent() {
        data object CardPresentCollectInteracPaymentFailed : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_collect_interac_payment_failed"
        }

        data object CardPresentCollectInteracPaymentSuccess : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_collect_interac_payment_success"
        }

        data object CardPresentCollectInteracRefundCancelled : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_collect_interac_refund_cancelled"
        }

        data object CardPresentCollectPaymentCancelled : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_collect_payment_cancelled"
        }

        data object CardPresentCollectPaymentFailed : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_collect_payment_failed"
        }

        data object CardPresentCollectPaymentSuccess : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_collect_payment_success"
        }

        data object CardPresentConnectionLearnMoreTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_connection_learn_more_tapped"
        }

        data object CardPresentOnboardingCompleted : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_onboarding_completed"
        }

        data object CardPresentOnboardingCtaFailed : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_onboarding_cta_failed"
        }

        data object CardPresentOnboardingCtaTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_onboarding_cta_tapped"
        }

        data object CardPresentOnboardingLearnMoreTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_onboarding_learn_more_tapped"
        }

        data object CardPresentOnboardingNotCompleted : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_onboarding_not_completed"
        }

        data object CardPresentOnboardingStepSkipped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_onboarding_step_skipped"
        }

        data object CardPresentPaymentFailedContactSupportTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_payment_failed_contact_support_tapped"
        }

        data object CardPresentPaymentGatewaySelected : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_payment_gateway_selected"
        }

        data object CardPresentSelectReaderTypeBluetoothTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_select_reader_type_bluetooth_tapped"
        }

        data object CardPresentSelectReaderTypeBuiltInTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_select_reader_type_built_in_tapped"
        }

        data object CardPresentTapToPayNotAvailable : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_tap_to_pay_not_available"
        }

        data object CardPresentTapToPayPaymentFailedEnableNfcTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_present_tap_to_pay_payment_failed_enable_nfc_tapped"
        }

        data object CardReaderAutomaticDisconnect : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_automatic_disconnect"
        }

        data object CardReaderAutoConnectionStarted : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_auto_connection_started"
        }

        data object CardReaderConnectionFailed : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_connection_failed"
        }

        data object CardReaderConnectionSuccess : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_connection_success"
        }

        data object CardReaderConnectionTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_connection_tapped"
        }

        data object CardReaderDisconnectTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_disconnect_tapped"
        }

        data object CardReaderDiscoveryFailed : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_discovery_failed"
        }

        data object CardReaderDiscoveryReaderDiscovered : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_discovery_reader_discovered"
        }

        data object CardReaderDiscoveryTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_discovery_tapped"
        }

        data object CardReaderLocationFailure : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_location_failure"
        }

        data object CardReaderLocationMissingTapped : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_location_missing_tapped"
        }

        data object CardReaderLocationSuccess : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_location_success"
        }

        data object CardReaderSoftwareUpdateAlertInstallClicked : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_software_update_alert_install_clicked"
        }

        data object CardReaderSoftwareUpdateAlertShown : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_software_update_alert_shown"
        }

        data object CardReaderSoftwareUpdateFailed : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_software_update_failed"
        }

        data object CardReaderSoftwareUpdateStarted : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_software_update_started"
        }

        data object CardReaderSoftwareUpdateSuccess : PaymentFlowTrackerEvent() {
            override val name: String = "card_reader_software_update_success"
        }

        data object DisableCashOnDeliveryFailed : PaymentFlowTrackerEvent() {
            override val name: String = "disable_cash_on_delivery_failed"
        }

        data object DisableCashOnDeliverySuccess : PaymentFlowTrackerEvent() {
            override val name: String = "disable_cash_on_delivery_success"
        }

        data object EnableCashOnDeliveryFailed : PaymentFlowTrackerEvent() {
            override val name: String = "enable_cash_on_delivery_failed"
        }

        data object EnableCashOnDeliverySuccess : PaymentFlowTrackerEvent() {
            override val name: String = "enable_cash_on_delivery_success"
        }

        data object InPersonPaymentsLearnMoreTapped : PaymentFlowTrackerEvent() {
            override val name: String = "in_person_payments_learn_more_tapped"
        }

        data object ManageCardReadersAutomaticDisconnectBuiltInReader : PaymentFlowTrackerEvent() {
            override val name: String = "manage_card_readers_automatic_disconnect_built_in_reader"
        }

        data object PaymentsFlowOrderCollectPaymentTapped : PaymentFlowTrackerEvent() {
            override val name: String = "payments_flow_order_collect_payment_tapped"
        }

        data object PaymentsHubCashOnDeliveryToggled : PaymentFlowTrackerEvent() {
            override val name: String = "payments_hub_cash_on_delivery_toggled"
        }

        data object PaymentsHubCashOnDeliveryToggledLearnMoreTapped : PaymentFlowTrackerEvent() {
            override val name: String = "payments_hub_cash_on_delivery_toggled_learn_more_tapped"
        }

        data object PaymentsOnboardingDismissed : PaymentFlowTrackerEvent() {
            override val name: String = "payments_onboarding_dismissed"
        }

        data object PaymentsOnboardingShown : PaymentFlowTrackerEvent() {
            override val name: String = "payments_onboarding_shown"
        }

        data object ReceiptEmailFailed : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_email_failed"
        }

        data object ReceiptEmailTapped : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_email_tapped"
        }

        data object ReceiptPrintCanceled : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_print_canceled"
        }

        data object ReceiptPrintFailed : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_print_failed"
        }

        data object ReceiptPrintSuccess : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_print_success"
        }

        data object ReceiptPrintTapped : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_print_tapped"
        }

        data object ReceiptUrlFetchingFails : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_url_fetching_fails"
        }

        data object ReceiptViewTapped : PaymentFlowTrackerEvent() {
            override val name: String = "receipt_view_tapped"
        }

        data object PaymentsFlowFailed : PaymentFlowTrackerEvent() {
            override val name: String = "payments_flow_failed"
        }

        data object PaymentsFlowCanceled : PaymentFlowTrackerEvent() {
            override val name: String = "payments_flow_canceled"
        }

        data object PaymentsFlowCollect : PaymentFlowTrackerEvent() {
            override val name: String = "payments_flow_collect"
        }

        data object PaymentsFlowCompleted : PaymentFlowTrackerEvent() {
            override val name: String = "payments_flow_completed"
        }
    }
}

internal fun IAnalyticsEvent.addProperties(additionalProperties: Map<String, String>) {
    when (this) {
        is WooPosAnalyticsEvent -> addProperties(additionalProperties)
        else -> error("Cannot add properties to non-WooPosAnalytics event")
    }
}
