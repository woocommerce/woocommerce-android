package com.woocommerce.android.ui.prefs.cardreader

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.*
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Failed
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.prefs.cardreader.onboarding.PluginType
import javax.inject.Inject

class CardReaderTracker @Inject constructor(
    private val trackerWrapper: AnalyticsTrackerWrapper,
) {
    fun trackOnboardingLearnMoreTapped() {
        trackerWrapper.track(CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED)
    }

    fun trackOnboardingState(state: CardReaderOnboardingState) {
        getOnboardingNotCompletedReason(state)?.let {
            trackerWrapper.track(CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to it))
        }
    }

    @Suppress("ComplexMethod")
    private fun getOnboardingNotCompletedReason(state: CardReaderOnboardingState): String? =
        when (state) {
            is CardReaderOnboardingState.OnboardingCompleted -> null
            is CardReaderOnboardingState.StoreCountryNotSupported -> "country_not_supported"
            CardReaderOnboardingState.StripeAccountOverdueRequirement -> "account_overdue_requirements"
            is CardReaderOnboardingState.StripeAccountPendingRequirement -> "account_pending_requirements"
            CardReaderOnboardingState.StripeAccountRejected -> "account_rejected"
            CardReaderOnboardingState.StripeAccountUnderReview -> "account_under_review"
            is CardReaderOnboardingState.StripeAccountCountryNotSupported -> "account_country_not_supported"
            CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount -> "wcpay_in_test_mode_with_live_account"
            CardReaderOnboardingState.WcpayNotActivated -> "wcpay_not_activated"
            CardReaderOnboardingState.WcpayNotInstalled -> "wcpay_not_installed"
            is CardReaderOnboardingState.SetupNotCompleted ->
                "${getPluginNameReasonPrefix(state.pluginType)}_not_setup"
            is CardReaderOnboardingState.PluginUnsupportedVersion ->
                "${getPluginNameReasonPrefix(state.pluginType)}_unsupported_version"
            CardReaderOnboardingState.GenericError -> "generic_error"
            CardReaderOnboardingState.NoConnectionError -> "no_connection_error"
            CardReaderOnboardingState.WcpayAndStripeActivated -> "wcpay_and_stripe_installed_and_activated"
        }

    private fun getPluginNameReasonPrefix(pluginType: PluginType): String {
        return when (pluginType) {
            PluginType.WOOCOMMERCE_PAYMENTS -> "wcpay"
            PluginType.STRIPE_EXTENSION_GATEWAY -> "stripe_extension"
        }
    }

    fun trackSoftwareUpdateStarted(requiredUpdate: Boolean) {
        trackSoftwareUpdateEvent(CARD_READER_SOFTWARE_UPDATE_STARTED, requiredUpdate)
    }

    fun trackSoftwareUpdateUnknownStatus() {
        trackerWrapper.track(
            CARD_READER_SOFTWARE_UPDATE_FAILED,
            this.javaClass.simpleName,
            null,
            "Unknown software update status"
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

    private fun trackSoftwareUpdateEvent(
        event: AnalyticsTracker.Stat,
        requiredUpdate: Boolean,
        errorDescription: String? = null,
    ) {
        val eventPropertiesMap = errorDescription?.let { description ->
            hashMapOf(
                AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to if (requiredUpdate) REQUIRED_UPDATE else OPTIONAL_UPDATE,
                AnalyticsTracker.KEY_ERROR_CONTEXT to this.javaClass.simpleName,
                AnalyticsTracker.KEY_ERROR_DESC to description
            )
        } ?: run {
            hashMapOf(
                AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to if (requiredUpdate) REQUIRED_UPDATE else OPTIONAL_UPDATE
            )
        }
        trackerWrapper.track(
            event,
            eventPropertiesMap
        )
    }

    companion object {
        private const val OPTIONAL_UPDATE: String = "Optional"
        private const val REQUIRED_UPDATE: String = "Required"
    }
}
