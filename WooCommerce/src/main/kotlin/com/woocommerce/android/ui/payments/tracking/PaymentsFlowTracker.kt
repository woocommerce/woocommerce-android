package com.woocommerce.android.ui.payments.tracking

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CASH_ON_DELIVERY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_DESC
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HORIZONTAL_SIZE_CLASS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_IS_ENABLED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PAYMENT_GATEWAY
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_POS_ONBOARDING_STATE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_REASON
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.IAnalyticsEvent
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.RefundStatusErrorType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Generic
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tracker.OrderDurationRecorder
import com.woocommerce.android.ui.payments.cardreader.cardReaderBatteryLevelPercent
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.OnboardingCtaReasonTapped
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewModel.CashOnDeliverySource
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptShare
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus.Result.NotAvailable
import javax.inject.Inject

class PaymentsFlowTracker @Inject constructor(
    private val trackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val cardReaderTrackingInfoProvider: CardReaderTrackingInfoProvider,
    private val paymentReceiptHelper: PaymentReceiptHelper,
    private val eventProvider: PaymentsFlowTrackerEventProvider,
) {
    @VisibleForTesting
    fun track(
        stat: IAnalyticsEvent,
        properties: MutableMap<String, Any> = mutableMapOf(),
        errorType: String? = null,
        errorDescription: String? = null,
    ) {
        addPreferredPluginSlugProperty(properties)
        addStoreCountryCodeProperty(properties)
        addCurrencyProperty(properties)
        addPaymentMethodTypeProperty(properties)
        addCardReaderModelProperty(properties)
        addCardReaderBatteryLevelProperty(properties)

        val isError = !errorType.isNullOrBlank() || !errorDescription.isNullOrEmpty()
        if (isError) {
            trackerWrapper.track(
                stat,
                properties,
                this@PaymentsFlowTracker.javaClass.simpleName,
                errorType,
                errorDescription
            )
        } else {
            trackerWrapper.track(stat, properties)
        }
    }

    private fun addCardReaderBatteryLevelProperty(properties: MutableMap<String, Any>) {
        cardReaderTrackingInfoProvider.trackingInfo.cardReaderBatteryLevelPercent?.let { batteryLevelPercent ->
            properties["card_reader_battery_level"] = "$batteryLevelPercent %"
        }
    }

    private fun addPreferredPluginSlugProperty(properties: MutableMap<String, Any>) {
        val site = selectedSite.get()
        val preferredPlugin = appPrefsWrapper.getCardReaderPreferredPlugin(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId
        )
        properties["plugin_slug"] = when (preferredPlugin) {
            WOOCOMMERCE_PAYMENTS -> "woocommerce-payments"
            STRIPE_EXTENSION_GATEWAY -> "woocommerce-stripe"
            null -> "unknown"
        }
    }

    private fun addStoreCountryCodeProperty(properties: MutableMap<String, Any>) {
        properties["country"] = cardReaderTrackingInfoProvider.trackingInfo.country ?: "unknown"
    }

    private fun addCurrencyProperty(properties: MutableMap<String, Any>) {
        val currency = cardReaderTrackingInfoProvider.trackingInfo.currency
        if (!currency.isNullOrBlank()) {
            properties["currency"] = currency
        }
    }

    private fun addPaymentMethodTypeProperty(properties: MutableMap<String, Any>) {
        val paymentMethodType = cardReaderTrackingInfoProvider.trackingInfo.paymentMethodType
        if (!paymentMethodType.isNullOrBlank()) {
            properties["payment_method_type"] = paymentMethodType
        }
    }

    private fun addCardReaderModelProperty(properties: MutableMap<String, Any>) {
        val cardReaderModel = cardReaderTrackingInfoProvider.trackingInfo.cardReaderModel
        if (!cardReaderModel.isNullOrBlank()) {
            properties["card_reader_model"] = cardReaderModel
        }
    }

    private fun getReceiptSource(): Pair<String, String> =
        if (paymentReceiptHelper.isWCCanGenerateReceipts()) {
            AnalyticsTracker.KEY_SOURCE to "backend"
        } else {
            AnalyticsTracker.KEY_SOURCE to "local"
        }

    @Suppress("ComplexMethod")
    private fun getOnboardingNotCompletedReason(state: CardReaderOnboardingState): String? =
        when (state) {
            is CardReaderOnboardingState.OnboardingCompleted -> null
            is CardReaderOnboardingState.StoreCountryNotSupported -> "country_not_supported"
            is CardReaderOnboardingState.PluginIsNotSupportedInTheCountry ->
                "${getPluginNameReasonPrefix(state.preferredPlugin)}_is_not_supported_in_${state.countryCode}"

            is CardReaderOnboardingState.StripeAccountOverdueRequirement -> "account_overdue_requirements"
            is CardReaderOnboardingState.StripeAccountPendingRequirement -> "account_pending_requirements"
            is CardReaderOnboardingState.StripeAccountRejected -> "account_rejected"
            is CardReaderOnboardingState.StripeAccountUnderReview -> "account_under_review"
            is CardReaderOnboardingState.StripeAccountCountryNotSupported -> "account_country_not_supported"
            is CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount ->
                "${getPluginNameReasonPrefix(state.preferredPlugin)}_in_test_mode_with_live_account"

            is CardReaderOnboardingState.WcpayNotActivated -> "wcpay_not_activated"
            is CardReaderOnboardingState.WcpayNotInstalled -> "wcpay_not_installed"
            is CardReaderOnboardingState.SetupNotCompleted ->
                "${getPluginNameReasonPrefix(state.preferredPlugin)}_not_setup"

            is CardReaderOnboardingState.PluginUnsupportedVersion ->
                "${getPluginNameReasonPrefix(state.preferredPlugin)}_unsupported_version"

            is CardReaderOnboardingState.GenericError -> "generic_error"
            is CardReaderOnboardingState.NoConnectionError -> "no_connection_error"
            CardReaderOnboardingState.ChoosePaymentGatewayProvider ->
                "multiple_payment_providers_conflict"

            is CardReaderOnboardingState.CashOnDeliveryDisabled -> "cash_on_delivery_disabled"
        }

    private fun getPluginNameReasonPrefix(pluginType: PluginType): String {
        return when (pluginType) {
            WOOCOMMERCE_PAYMENTS -> "wcpay"
            STRIPE_EXTENSION_GATEWAY -> "stripe_extension"
        }
    }

    fun trackOnboardingLearnMoreTapped() {
        track(eventProvider.CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED)
    }

    fun trackOnboardingState(state: CardReaderOnboardingState) {
        when (state) {
            is CardReaderOnboardingState.OnboardingCompleted -> track(eventProvider.CARD_PRESENT_ONBOARDING_COMPLETED)
            else -> getOnboardingNotCompletedReason(state)?.let {
                track(eventProvider.CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mutableMapOf(KEY_REASON to it))
            }
        }
    }

    fun trackOnboardingSkippedState(state: CardReaderOnboardingState) {
        getOnboardingNotCompletedReason(state)?.let {
            track(
                eventProvider.CARD_PRESENT_ONBOARDING_STEP_SKIPPED,
                mutableMapOf(
                    KEY_REASON to it,
                    "remind_later" to false
                )
            )
        }
    }

    fun trackOnboardingCtaTapped(reason: OnboardingCtaReasonTapped) {
        track(
            eventProvider.CARD_PRESENT_ONBOARDING_CTA_TAPPED,
            mutableMapOf(KEY_REASON to reason.value)
        )
    }

    fun trackOnboardingCtaFailed(reason: OnboardingCtaReasonTapped, description: String) {
        track(
            eventProvider.CARD_PRESENT_ONBOARDING_CTA_FAILED,
            mutableMapOf(
                KEY_REASON to reason.value,
                KEY_ERROR_DESC to description,
            )
        )
    }

    fun trackCashOnDeliveryToggled(isEnabled: Boolean) {
        track(
            eventProvider.PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED,
            mutableMapOf(
                KEY_IS_ENABLED to isEnabled
            )
        )
    }

    fun trackCashOnDeliveryEnabledSuccess(source: CashOnDeliverySource) {
        track(
            eventProvider.ENABLE_CASH_ON_DELIVERY_SUCCESS,
            mutableMapOf(
                KEY_CASH_ON_DELIVERY_SOURCE to source.toString()
            )
        )
    }

    fun trackCashOnDeliveryEnabledFailure(source: CashOnDeliverySource, errorMessage: String?) {
        track(
            eventProvider.ENABLE_CASH_ON_DELIVERY_FAILED,
            errorDescription = errorMessage,
            properties = mutableMapOf(
                KEY_CASH_ON_DELIVERY_SOURCE to source.toString()
            )
        )
    }

    fun trackCashOnDeliveryDisabledSuccess(source: CashOnDeliverySource) {
        track(
            eventProvider.DISABLE_CASH_ON_DELIVERY_SUCCESS,
            mutableMapOf(
                KEY_CASH_ON_DELIVERY_SOURCE to source.toString()
            )
        )
    }

    fun trackCashOnDeliveryDisabledFailure(source: CashOnDeliverySource, errorMessage: String?) {
        track(
            eventProvider.DISABLE_CASH_ON_DELIVERY_FAILED,
            errorDescription = errorMessage,
            properties = mutableMapOf(
                KEY_CASH_ON_DELIVERY_SOURCE to source.toString()
            )
        )
    }

    fun trackCashOnDeliveryLearnMoreTapped() {
        track(eventProvider.PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED_LEARN_MORE_TAPPED)
    }

    fun trackPaymentGatewaySelected(pluginType: PluginType) {
        val preferredPlugin = when (pluginType) {
            WOOCOMMERCE_PAYMENTS -> "woocommerce-payments"
            STRIPE_EXTENSION_GATEWAY -> "woocommerce-stripe-gateway"
        }
        track(
            eventProvider.CARD_PRESENT_PAYMENT_GATEWAY_SELECTED,
            mutableMapOf(KEY_PAYMENT_GATEWAY to preferredPlugin)
        )
    }

    fun trackSoftwareUpdateStarted(requiredUpdate: Boolean) {
        trackSoftwareUpdateEvent(eventProvider.CARD_READER_SOFTWARE_UPDATE_STARTED, requiredUpdate)
    }

    fun trackSoftwareUpdateAlertShown() {
        track(eventProvider.CARD_READER_SOFTWARE_UPDATE_ALERT_SHOWN)
    }

    fun trackSoftwareUpdateAlertInstallClicked() {
        track(eventProvider.CARD_READER_SOFTWARE_UPDATE_ALERT_INSTALL_CLICKED)
    }

    fun trackSoftwareUpdateUnknownStatus() {
        track(
            eventProvider.CARD_READER_SOFTWARE_UPDATE_FAILED,
            errorDescription = "Unknown software update status"
        )
    }

    fun trackSoftwareUpdateSucceeded(requiredUpdate: Boolean) {
        trackSoftwareUpdateEvent(eventProvider.CARD_READER_SOFTWARE_UPDATE_SUCCESS, requiredUpdate)
    }

    fun trackSoftwareUpdateFailed(status: Failed, requiredUpdate: Boolean) {
        trackSoftwareUpdateEvent(eventProvider.CARD_READER_SOFTWARE_UPDATE_FAILED, requiredUpdate, status.message)
    }

    fun trackSoftwareUpdateCancelled(requiredUpdate: Boolean) {
        trackSoftwareUpdateEvent(
            eventProvider.CARD_READER_SOFTWARE_UPDATE_FAILED,
            requiredUpdate,
            "User manually cancelled the flow"
        )
    }

    fun trackReadersDiscovered(count: Int) {
        track(
            eventProvider.CARD_READER_DISCOVERY_READER_DISCOVERED,
            mutableMapOf("reader_count" to count)
        )
    }

    fun trackReaderDiscoveryFailed(errorMessage: String) {
        track(
            eventProvider.CARD_READER_DISCOVERY_FAILED,
            errorDescription = errorMessage
        )
    }

    fun trackDiscoveryTapped() {
        track(eventProvider.CARD_READER_DISCOVERY_TAPPED)
    }

    fun trackAutoConnectionStarted() {
        track(eventProvider.CARD_READER_AUTO_CONNECTION_STARTED)
    }

    fun trackOnConnectTapped() {
        track(eventProvider.CARD_READER_CONNECTION_TAPPED)
    }

    fun trackFetchingLocationSucceeded() {
        track(eventProvider.CARD_READER_LOCATION_SUCCESS)
    }

    fun trackFetchingLocationFailed(errorDescription: String?) {
        track(
            eventProvider.CARD_READER_LOCATION_FAILURE,
            errorDescription = errorDescription,
        )
    }

    fun trackMissingLocationTapped() {
        track(eventProvider.CARD_READER_LOCATION_MISSING_TAPPED)
    }

    fun trackConnectionFailed() {
        track(eventProvider.CARD_READER_CONNECTION_FAILED)
    }

    fun trackConnectionSucceeded() {
        track(eventProvider.CARD_READER_CONNECTION_SUCCESS)
    }

    fun trackPaymentFailed(errorMessage: String, errorType: CardPaymentStatusErrorType = Generic) {
        track(
            eventProvider.CARD_PRESENT_COLLECT_PAYMENT_FAILED,
            errorType = errorType.toString(),
            errorDescription = errorMessage
        )
    }

    fun trackPaymentSucceeded() {
        track(eventProvider.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS, getAndResetFlowsDuration())
    }

    fun trackInteracPaymentSucceeded() {
        track(eventProvider.CARD_PRESENT_COLLECT_INTERAC_PAYMENT_SUCCESS, getAndResetFlowsDuration())
    }

    fun trackInteracPaymentFailed(
        orderId: Long,
        errorMessage: String,
        errorType: RefundStatusErrorType = RefundStatusErrorType.Generic
    ) {
        track(
            eventProvider.CARD_PRESENT_COLLECT_INTERAC_PAYMENT_FAILED,
            properties = mutableMapOf("orderId" to orderId),
            errorType = errorType.toString(),
            errorDescription = errorMessage
        )
    }

    fun trackPrintReceiptTapped() {
        track(
            eventProvider.RECEIPT_PRINT_TAPPED,
            properties = mutableMapOf(getReceiptSource())
        )
    }

    fun trackEmailReceiptTapped() {
        track(
            eventProvider.RECEIPT_EMAIL_TAPPED,
            properties = mutableMapOf(getReceiptSource())
        )
    }

    fun trackPrintReceiptCancelled() {
        track(
            eventProvider.RECEIPT_PRINT_CANCELED,
            properties = mutableMapOf(getReceiptSource())
        )
    }

    fun trackPrintReceiptFailed() {
        track(
            eventProvider.RECEIPT_PRINT_FAILED,
            properties = mutableMapOf(getReceiptSource())
        )
    }

    fun trackPrintReceiptSucceeded() {
        track(
            eventProvider.RECEIPT_PRINT_SUCCESS,
            properties = mutableMapOf(getReceiptSource())
        )
    }

    fun trackReceiptViewTapped(properties: Map<String, Any>) {
        track(
            eventProvider.RECEIPT_VIEW_TAPPED,
            properties = properties.toMutableMap().also {
                it.putAll(
                    mapOf(getReceiptSource())
                )
            }
        )
    }

    fun trackReceiptUrlFetchingFails(errorDescription: String) {
        track(
            eventProvider.RECEIPT_URL_FETCHING_FAILS,
            properties = mutableMapOf(getReceiptSource()),
            errorDescription = errorDescription,
        )
    }

    fun trackPaymentCancelled(currentPaymentState: String?) {
        track(
            eventProvider.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED,
            errorDescription = "User manually cancelled the payment during state $currentPaymentState"
        )
    }

    fun trackCollectPaymentTapped(deviceType: String) {
        trackerWrapper.track(
            eventProvider.PAYMENTS_FLOW_ORDER_COLLECT_PAYMENT_TAPPED,
            mapOf(KEY_HORIZONTAL_SIZE_CLASS to deviceType)
        )
    }

    fun trackDisconnectTapped() {
        track(eventProvider.CARD_READER_DISCONNECT_TAPPED)
    }

    private fun trackSoftwareUpdateEvent(
        event: IAnalyticsEvent,
        requiredUpdate: Boolean,
        errorDescription: String? = null,
    ) {
        val eventPropertiesMap = hashMapOf<String, Any>(
            AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to if (requiredUpdate) REQUIRED_UPDATE else OPTIONAL_UPDATE,
        )
        track(
            event,
            eventPropertiesMap,
            errorDescription = errorDescription
        )
    }

    fun trackInteracRefundCancelled(currentRefundState: String?) {
        track(
            eventProvider.CARD_PRESENT_COLLECT_INTERAC_REFUND_CANCELLED,
            errorDescription = "User manually cancelled the payment during state $currentRefundState"
        )
    }

    fun trackLearnMoreConnectionClicked() {
        track(eventProvider.CARD_PRESENT_CONNECTION_LEARN_MORE_TAPPED)
    }

    fun trackIPPLearnMoreClicked(source: String) {
        track(
            stat = eventProvider.IN_PERSON_PAYMENTS_LEARN_MORE_TAPPED,
            properties = mutableMapOf(AnalyticsTracker.IPP_LEARN_MORE_SOURCE to source)
        )
    }

    fun trackSelectReaderTypeBuiltInTapped() {
        track(eventProvider.CARD_PRESENT_SELECT_READER_TYPE_BUILT_IN_TAPPED)
    }

    fun trackSelectReaderTypeBluetoothTapped() {
        track(eventProvider.CARD_PRESENT_SELECT_READER_TYPE_BLUETOOTH_TAPPED)
    }

    fun trackManageCardReadersAutomaticDisconnectOfBuiltInReader() {
        track(eventProvider.MANAGE_CARD_READERS_AUTOMATIC_DISCONNECT_BUILT_IN_READER)
    }

    fun trackTapToPayNotAvailableReason(
        reason: NotAvailable,
        source: String,
    ) {
        track(
            eventProvider.CARD_PRESENT_TAP_TO_PAY_NOT_AVAILABLE,
            properties = mutableMapOf(
                KEY_REASON to reason::class.java.simpleName,
                AnalyticsTracker.KEY_SOURCE to source,
            )
        )
    }

    fun trackAutomaticReadDisconnectWhenConnectedAnotherType() {
        track(eventProvider.CARD_READER_AUTOMATIC_DISCONNECT)
    }

    fun trackPaymentFailedContactSupportTapped() {
        track(eventProvider.CARD_PRESENT_PAYMENT_FAILED_CONTACT_SUPPORT_TAPPED)
    }

    fun trackPaymentFailedEnabledNfcTapped() {
        track(eventProvider.CARD_PRESENT_TAP_TO_PAY_PAYMENT_FAILED_ENABLE_NFC_TAPPED)
    }

    fun trackPaymentsFlowFailed(source: String, flow: String) {
        track(
            eventProvider.PAYMENTS_FLOW_FAILED,
            properties = mutableMapOf(
                AnalyticsTracker.KEY_SOURCE to source,
                AnalyticsTracker.KEY_FLOW to flow,
            )
        )
    }

    fun trackPaymentsFlowCanceled(flow: String) {
        track(
            eventProvider.PAYMENTS_FLOW_CANCELED,
            properties = mutableMapOf(AnalyticsTracker.KEY_FLOW to flow)
        )
    }

    fun trackPaymentsFlowCollect(
        flow: String,
        paymentMethod: String,
        orderId: Long,
        cardReaderType: String?,
        timeElapsed: Long?,
    ) {
        track(
            eventProvider.PAYMENTS_FLOW_COLLECT,
            properties = mutableMapOf<String, Any>(
                AnalyticsTracker.KEY_ORDER_ID to orderId,
                AnalyticsTracker.KEY_FLOW to flow,
                AnalyticsTracker.KEY_PAYMENT_METHOD to paymentMethod,
            ).also {
                if (cardReaderType != null) {
                    it[AnalyticsTracker.KEY_PAYMENT_CARD_READER_TYPE] = cardReaderType
                }
                if (timeElapsed != null) {
                    it[AnalyticsTracker.KEY_TIME_ELAPSED_SINCE_ADD_NEW_ORDER_IN_MILLIS] = timeElapsed.toString()
                }
            }
        )
    }

    fun trackPaymentsFlowCompleted(
        flow: String,
        paymentMethod: String,
        orderId: Long,
        amount: String,
        amountNormalized: Long,
    ) {
        track(
            eventProvider.PAYMENTS_FLOW_COMPLETED,
            properties = mutableMapOf(
                AnalyticsTracker.KEY_FLOW to flow,
                AnalyticsTracker.KEY_PAYMENT_METHOD to paymentMethod,
                AnalyticsTracker.KEY_ORDER_ID to orderId,
                AnalyticsTracker.KEY_AMOUNT to amount,
                AnalyticsTracker.KEY_AMOUNT_NORMALIZED to amountNormalized,
            )
        )
    }

    fun trackPaymentsReceiptSharingFailed(sharingResult: PaymentReceiptShare.ReceiptShareResult.Error) {
        when (sharingResult) {
            is PaymentReceiptShare.ReceiptShareResult.Error.FileCreation -> {
                track(
                    eventProvider.RECEIPT_EMAIL_FAILED,
                    errorType = "file_creation_failed",
                    errorDescription = "File creation failed",
                    properties = mutableMapOf(getReceiptSource())
                )
            }

            is PaymentReceiptShare.ReceiptShareResult.Error.FileDownload -> {
                track(
                    eventProvider.RECEIPT_EMAIL_FAILED,
                    errorType = "file_download_failed",
                    errorDescription = "File download failed",
                    properties = mutableMapOf(getReceiptSource())
                )
            }

            is PaymentReceiptShare.ReceiptShareResult.Error.Sharing -> {
                track(
                    eventProvider.RECEIPT_EMAIL_FAILED,
                    errorType = "no_app_found",
                    errorDescription = sharingResult.exception.message,
                    properties = mutableMapOf(getReceiptSource())
                )
            }
        }
    }

    fun trackOnboardingShownInPosFlow(state: CardReaderOnboardingState) {
        track(
            eventProvider.PAYMENTS_ONBOARDING_SHOWN,
            mutableMapOf(KEY_POS_ONBOARDING_STATE to (getOnboardingNotCompletedReason(state) ?: "completed"))
        )
    }

    fun trackOnboardingDismissedInPosFlow(state: CardReaderOnboardingState) {
        track(
            eventProvider.PAYMENTS_ONBOARDING_DISMISSED,
            mutableMapOf(KEY_POS_ONBOARDING_STATE to (getOnboardingNotCompletedReason(state) ?: "completed"))
        )
    }

    private fun getAndResetFlowsDuration(): MutableMap<String, Any> {
        val result = mutableMapOf<String, Any>()
            .also { mutableMap ->
                OrderDurationRecorder.millisecondsSinceOrderAddNew().getOrNull()?.let { timeElapsed ->
                    mutableMap[AnalyticsTracker.KEY_TIME_ELAPSED_SINCE_ADD_NEW_ORDER_IN_MILLIS] = timeElapsed.toString()
                }
                OrderDurationRecorder.millisecondsSinceCardPaymentStarted().getOrNull()?.let { timeElapsed ->
                    mutableMap[AnalyticsTracker.KEY_TIME_ELAPSED_SINCE_CARD_COLLECT_PAYMENT_IN_MILLIS] =
                        timeElapsed.toString()
                }
            }
        OrderDurationRecorder.reset()
        return result
    }

    companion object {
        private const val OPTIONAL_UPDATE = "Optional"
        private const val REQUIRED_UPDATE = "Required"
    }
}
