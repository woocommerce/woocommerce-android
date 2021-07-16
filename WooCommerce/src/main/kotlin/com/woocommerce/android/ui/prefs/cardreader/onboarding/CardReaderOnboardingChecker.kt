package com.woocommerce.android.ui.prefs.cardreader.onboarding

import javax.inject.Inject

@Suppress("TooManyFunctions")
class CardReaderOnboardingChecker @Inject constructor() {
    suspend fun getOnboardingState(): CardReaderOnboardingState {
        return when {
            !isCountrySupported() -> CardReaderOnboardingState.COUNTRY_NOT_SUPPORTED
            !isWCPayInstalled() -> CardReaderOnboardingState.WCPAY_NOT_INSTALLED
            !isWCPayVersionSupported() -> CardReaderOnboardingState.WCPAY_UNSUPPORTED_VERSION
            !isWCPayActivated() -> CardReaderOnboardingState.WCPAY_NOT_ACTIVATED
            !isWCPaySetupCompleted() -> CardReaderOnboardingState.WCPAY_SETUP_NOT_COMPLETED
            isWCPayInTestModeWithLiveStripeAccount() ->
                CardReaderOnboardingState.WCPAY_IN_TEST_MODE_WITH_LIVE_STRIPE_ACCOUNT
            isStripeAccountUnderReview() -> CardReaderOnboardingState.STRIPE_ACCOUNT_UNDER_REVIEW
            isStripeAccountPendingRequirements() -> CardReaderOnboardingState.STRIPE_ACCOUNT_PENDING_REQUIREMENT
            isStripeAccountOverdueRequirements() -> CardReaderOnboardingState.STRIPE_ACCOUNT_OVERDUE_REQUIREMENT
            isStripeAccountRejected() -> CardReaderOnboardingState.STRIPE_ACCOUNT_REJECTED
            else -> CardReaderOnboardingState.ONBOARDING_COMPLETED
        }
    }

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isCountrySupported(): Boolean = true

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isWCPayInstalled(): Boolean = true

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isWCPayVersionSupported(): Boolean = true

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isWCPayActivated(): Boolean = true

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isWCPaySetupCompleted(): Boolean = true

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isWCPayInTestModeWithLiveStripeAccount(): Boolean = false

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isStripeAccountUnderReview(): Boolean = false

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isStripeAccountPendingRequirements(): Boolean = false

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isStripeAccountOverdueRequirements(): Boolean = false

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isStripeAccountRejected(): Boolean = false
}

enum class CardReaderOnboardingState {
    ONBOARDING_COMPLETED,

    /**
     * Store is not located in one of the supported countries.
     */
    COUNTRY_NOT_SUPPORTED,

    /**
     * WCPay plugin is not installed on the store.
     */
    WCPAY_NOT_INSTALLED,

    /**
     * WCPay plugin is installed on the store, but the version is out-dated and doesn't contain required APIs
     * for card present payments.
     */
    WCPAY_UNSUPPORTED_VERSION,

    /**
     * WCPay is installed on the store but is not activated.
     */
    WCPAY_NOT_ACTIVATED,

    /**
     * WCPay is installed and activated but requires to be setup first.
     */
    WCPAY_SETUP_NOT_COMPLETED,

    /**
     * This is a bit special case: WCPay is set to "dev mode" but the connected Stripe account is in live mode.
     * Connecting to a reader or accepting payments is not supported in this state.
     */
    WCPAY_IN_TEST_MODE_WITH_LIVE_STRIPE_ACCOUNT,

    /**
     * The connected Stripe account has not been reviewed by Stripe yet. This is a temporary state and
     * the user needs to wait.
     */
    STRIPE_ACCOUNT_UNDER_REVIEW,

    /**
     * There are some pending requirements on the connected Stripe account. The merchant still has some time before the
     * deadline to fix them expires. In-person payments should work without issues.
     */
    STRIPE_ACCOUNT_PENDING_REQUIREMENT,

    /**
     * There are some overdue requirements on the connected Stripe account. Connecting to a reader or accepting
     * payments is not supported in this state.
     */
    STRIPE_ACCOUNT_OVERDUE_REQUIREMENT,

    /**
     * The Stripe account was rejected by Stripe. This can happen for example when the account is flagged as fraudulent
     * or the merchant violates the terms of service
     */
    STRIPE_ACCOUNT_REJECTED,

    /**
     * Generic error - for example, one of the requests failed.
     */
    GENERIC_ERROR,

    /**
     * Internet connection is not available.
     */
    NO_CONNECTION_ERROR
}
