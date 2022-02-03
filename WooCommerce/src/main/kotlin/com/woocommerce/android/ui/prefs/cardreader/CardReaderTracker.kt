package com.woocommerce.android.ui.prefs.cardreader

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
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
            trackerWrapper.track(AnalyticsTracker.Stat.CARD_PRESENT_ONBOARDING_NOT_COMPLETED, mapOf("reason" to it))
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
}
