package com.woocommerce.android.ui.payments.cardreader.onboarding

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class CardReaderOnboardingState(
    open val preferredPlugin: PluginType? = null
) : Parcelable {
    @Parcelize
    data class OnboardingCompleted(
        override val preferredPlugin: PluginType,
        val version: String?,
        val countryCode: String
    ) : CardReaderOnboardingState()

    /**
     * Store is not located in one of the supported countries.
     */
    @Parcelize
    data class StoreCountryNotSupported(val countryCode: String?) : CardReaderOnboardingState()

    /**
     * Preferred Plugin is not supported in the country
     */
    @Parcelize
    data class PluginIsNotSupportedInTheCountry(
        override val preferredPlugin: PluginType,
        val countryCode: String
    ) : CardReaderOnboardingState()

    /**
     * WCPay plugin is not installed on the store.
     */
    @Parcelize
    object WcpayNotInstalled : CardReaderOnboardingState(preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS)

    /**
     * Plugin is installed on the store, but the version is out-dated and doesn't contain required APIs
     * for card present payments.
     */
    @Parcelize
    data class PluginUnsupportedVersion(override val preferredPlugin: PluginType) : CardReaderOnboardingState()

    /**
     * WCPay is installed on the store but is not activated.
     */
    @Parcelize
    object WcpayNotActivated : CardReaderOnboardingState(preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS)

    /**
     * Plugin is installed and activated but requires to be setup first.
     */
    @Parcelize
    data class SetupNotCompleted(override val preferredPlugin: PluginType) : CardReaderOnboardingState()

    /**
     * Both plugins are installed and activated on the site. Merchant needs to choose their preferred payment
     * provider in this state.
     */
    @Parcelize
    object ChoosePaymentGatewayProvider : CardReaderOnboardingState()

    /**
     * This is a bit special case: WCPay is set to "dev mode" but the connected Stripe account is in live mode.
     * Connecting to a reader or accepting payments is not supported in this state.
     */
    @Parcelize
    data class PluginInTestModeWithLiveStripeAccount(override val preferredPlugin: PluginType) :
        CardReaderOnboardingState()

    /**
     * The connected Stripe account has not been reviewed by Stripe yet. This is a temporary state and
     * the user needs to wait.
     */
    @Parcelize
    data class StripeAccountUnderReview(override val preferredPlugin: PluginType) : CardReaderOnboardingState()

    /**
     * There are some pending requirements on the connected Stripe account. The merchant still has some time before the
     * deadline to fix them expires. In-Person Payments should work without issues. We pass along a PluginType for which
     * the Stripe account requirement is pending
     */
    @Parcelize
    data class StripeAccountPendingRequirement(
        val dueDate: Long?,
        override val preferredPlugin: PluginType,
        val version: String?,
        val countryCode: String,
    ) : CardReaderOnboardingState()

    /**
     * There are some overdue requirements on the connected Stripe account. Connecting to a reader or accepting
     * payments is not supported in this state.
     */
    @Parcelize
    data class StripeAccountOverdueRequirement(override val preferredPlugin: PluginType) : CardReaderOnboardingState()

    /**
     * The Stripe account was rejected by Stripe. This can happen for example when the account is flagged as fraudulent
     * or the merchant violates the terms of service
     */
    @Parcelize
    data class StripeAccountRejected(override val preferredPlugin: PluginType) : CardReaderOnboardingState()

    /**
     * The Stripe account is attached to an address in one of the unsupported countries.
     */
    @Parcelize
    data class StripeAccountCountryNotSupported(override val preferredPlugin: PluginType, val countryCode: String?) :
        CardReaderOnboardingState()

    /**
     * Generic error - for example, one of the requests failed.
     */
    @Parcelize
    object GenericError : CardReaderOnboardingState()

    /**
     * Internet connection is not available.
     */
    @Parcelize
    object NoConnectionError : CardReaderOnboardingState()

    /**
     * Payment type Cash on Delivery is disabled on the store.
     */
    @Parcelize
    data class CashOnDeliveryDisabled(
        val countryCode: String,
        override val preferredPlugin: PluginType,
        val version: String?,
    ) : CardReaderOnboardingState()
}
