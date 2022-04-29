package com.woocommerce.android.ui.cardreader

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_INTERAC_PAYMENT_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_INTERAC_PAYMENT_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_INTERAC_REFUND_CANCELLED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_COLLECT_PAYMENT_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_PRESENT_ONBOARDING_NOT_COMPLETED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_AUTO_CONNECTION_STARTED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_CONNECTION_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_CONNECTION_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_CONNECTION_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_DISCONNECT_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_DISCOVERY_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_DISCOVERY_READER_DISCOVERED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_DISCOVERY_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_LOCATION_FAILURE
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_LOCATION_MISSING_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_LOCATION_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_STARTED
import com.woocommerce.android.analytics.AnalyticsEvent.CARD_READER_SOFTWARE_UPDATE_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_EMAIL_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_EMAIL_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_CANCELED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.RECEIPT_PRINT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.RefundStatusErrorType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Generic
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import javax.inject.Inject

class CardReaderTracker @Inject constructor(
    private val trackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val cardReaderTrackingInfoProvider: CardReaderTrackingInfoProvider
) {
    @VisibleForTesting
    fun track(
        stat: AnalyticsEvent,
        properties: MutableMap<String, Any> = mutableMapOf(),
        errorType: String? = null,
        errorDescription: String? = null,
    ) {
        addPreferredPluginSlugProperty(properties)
        addStoreCountryCodeProperty(properties)
        addCurrencyProperty(properties)
        addPaymentMethodTypeProperty(properties)
        addCardReaderModelProperty(properties)

        val isError = !errorType.isNullOrBlank() || !errorDescription.isNullOrEmpty()
        if (isError) {
            trackerWrapper.track(
                stat,
                properties,
                this@CardReaderTracker.javaClass.simpleName,
                errorType,
                errorDescription
            )
        } else {
            trackerWrapper.track(stat, properties)
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
            is CardReaderOnboardingState.WcpayAndStripeActivated -> "wcpay_and_stripe_installed_and_activated"
        }

    private fun getPluginNameReasonPrefix(pluginType: PluginType): String {
        return when (pluginType) {
            WOOCOMMERCE_PAYMENTS -> "wcpay"
            STRIPE_EXTENSION_GATEWAY -> "stripe_extension"
        }
    }

    fun trackOnboardingLearnMoreTapped() {
        track(CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED)
    }

    fun trackOnboardingState(state: CardReaderOnboardingState) {
        getOnboardingNotCompletedReason(state)?.let {
            track(CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mutableMapOf("reason" to it))
        }
    }

    fun trackSoftwareUpdateStarted(requiredUpdate: Boolean) {
        trackSoftwareUpdateEvent(CARD_READER_SOFTWARE_UPDATE_STARTED, requiredUpdate)
    }

    fun trackSoftwareUpdateUnknownStatus() {
        track(
            CARD_READER_SOFTWARE_UPDATE_FAILED,
            errorDescription = "Unknown software update status"
        )
    }

    fun trackSoftwareUpdateSucceeded(requiredUpdate: Boolean) {
        trackSoftwareUpdateEvent(CARD_READER_SOFTWARE_UPDATE_SUCCESS, requiredUpdate)
    }

    fun trackSoftwareUpdateFailed(status: Failed, requiredUpdate: Boolean) {
        trackSoftwareUpdateEvent(CARD_READER_SOFTWARE_UPDATE_FAILED, requiredUpdate, status.message)
    }

    fun trackSoftwareUpdateCancelled(requiredUpdate: Boolean) {
        trackSoftwareUpdateEvent(
            CARD_READER_SOFTWARE_UPDATE_FAILED,
            requiredUpdate,
            "User manually cancelled the flow"
        )
    }

    fun trackReadersDiscovered(count: Int) {
        track(
            CARD_READER_DISCOVERY_READER_DISCOVERED,
            mutableMapOf("reader_count" to count)
        )
    }

    fun trackReaderDiscoveryFailed(errorMessage: String) {
        track(
            CARD_READER_DISCOVERY_FAILED,
            errorDescription = errorMessage
        )
    }

    fun trackDiscoveryTapped() {
        track(CARD_READER_DISCOVERY_TAPPED)
    }

    fun trackAutoConnectionStarted() {
        track(CARD_READER_AUTO_CONNECTION_STARTED)
    }

    fun trackOnConnectTapped() {
        track(CARD_READER_CONNECTION_TAPPED)
    }

    fun trackFetchingLocationSucceeded() {
        track(CARD_READER_LOCATION_SUCCESS)
    }

    fun trackFetchingLocationFailed(errorDescription: String?) {
        track(
            CARD_READER_LOCATION_FAILURE,
            errorDescription = errorDescription,
        )
    }

    fun trackMissingLocationTapped() {
        track(CARD_READER_LOCATION_MISSING_TAPPED)
    }

    fun trackConnectionFailed() {
        track(CARD_READER_CONNECTION_FAILED)
    }

    fun trackConnectionSucceeded() {
        track(CARD_READER_CONNECTION_SUCCESS)
    }

    fun trackPaymentFailed(errorMessage: String, errorType: CardPaymentStatusErrorType = Generic) {
        track(
            CARD_PRESENT_COLLECT_PAYMENT_FAILED,
            errorType = errorType.toString(),
            errorDescription = errorMessage
        )
    }

    fun trackPaymentSucceeded() {
        track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)
    }

    fun trackInteracPaymentSucceeded() {
        track(CARD_PRESENT_COLLECT_INTERAC_PAYMENT_SUCCESS)
    }

    fun trackInteracPaymentFailed(
        orderId: Long,
        errorMessage: String,
        errorType: RefundStatusErrorType = RefundStatusErrorType.Generic
    ) {
        track(
            CARD_PRESENT_COLLECT_INTERAC_PAYMENT_FAILED,
            properties = mutableMapOf("orderId" to orderId),
            errorType = errorType.toString(),
            errorDescription = errorMessage
        )
    }

    fun trackPrintReceiptTapped() {
        track(RECEIPT_PRINT_TAPPED)
    }

    fun trackEmailReceiptTapped() {
        track(RECEIPT_EMAIL_TAPPED)
    }

    fun trackEmailReceiptFailed() {
        track(RECEIPT_EMAIL_FAILED)
    }

    fun trackPrintReceiptCancelled() {
        track(RECEIPT_PRINT_CANCELED)
    }

    fun trackPrintReceiptFailed() {
        track(RECEIPT_PRINT_FAILED)
    }

    fun trackPrintReceiptSucceeded() {
        track(RECEIPT_PRINT_SUCCESS)
    }

    fun trackPaymentCancelled(currentPaymentState: String?) {
        track(
            CARD_PRESENT_COLLECT_PAYMENT_CANCELLED,
            errorDescription = "User manually cancelled the payment during state $currentPaymentState"
        )
    }

    fun trackCollectPaymentTapped() {
        track(CARD_PRESENT_COLLECT_PAYMENT_TAPPED)
    }

    fun trackDisconnectTapped() {
        track(CARD_READER_DISCONNECT_TAPPED)
    }

    private fun trackSoftwareUpdateEvent(
        event: AnalyticsEvent,
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
            CARD_PRESENT_COLLECT_INTERAC_REFUND_CANCELLED,
            errorDescription = "User manually cancelled the payment during state $currentRefundState"
        )
    }

    companion object {
        private const val OPTIONAL_UPDATE = "Optional"
        private const val REQUIRED_UPDATE = "Required"
    }
}
