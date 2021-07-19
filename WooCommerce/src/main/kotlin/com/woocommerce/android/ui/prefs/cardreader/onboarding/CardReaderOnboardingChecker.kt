package com.woocommerce.android.ui.prefs.cardreader.onboarding

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.pay.WCPaymentAccountResult
import org.wordpress.android.fluxc.store.WCPayStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

private val SUPPORTED_COUNTRIES = listOf("US")

@Suppress("TooManyFunctions")
class CardReaderOnboardingChecker @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooStore: WooCommerceStore,
    private val wcPayStore: WCPayStore,
    private val dispatchers: CoroutineDispatchers
) {
    suspend fun getOnboardingState(): CardReaderOnboardingState {
        if (!isCountrySupported())
            return CardReaderOnboardingState.COUNTRY_NOT_SUPPORTED
        if (!isWCPayInstalled())
            return CardReaderOnboardingState.WCPAY_NOT_INSTALLED
        if (!isWCPayVersionSupported())
            return CardReaderOnboardingState.WCPAY_UNSUPPORTED_VERSION
        if (!isWCPayActivated())
            return CardReaderOnboardingState.WCPAY_NOT_ACTIVATED

        val paymentAccount = wcPayStore.loadAccount(selectedSite.get()).model
            ?: return CardReaderOnboardingState.GENERIC_ERROR

        if (!isWCPaySetupCompleted(paymentAccount))
            return CardReaderOnboardingState.WCPAY_SETUP_NOT_COMPLETED
        if (isWCPayInTestModeWithLiveStripeAccount())
            return CardReaderOnboardingState.WCPAY_IN_TEST_MODE_WITH_LIVE_STRIPE_ACCOUNT
        if (isStripeAccountUnderReview(paymentAccount))
            return CardReaderOnboardingState.STRIPE_ACCOUNT_UNDER_REVIEW
        if (isStripeAccountPendingRequirements(paymentAccount))
            return CardReaderOnboardingState.STRIPE_ACCOUNT_PENDING_REQUIREMENT
        if (isStripeAccountOverdueRequirements(paymentAccount))
            return CardReaderOnboardingState.STRIPE_ACCOUNT_OVERDUE_REQUIREMENT
        if (isStripeAccountRejected(paymentAccount))
            return CardReaderOnboardingState.STRIPE_ACCOUNT_REJECTED
        if (isInUndefinedState(paymentAccount))
            return CardReaderOnboardingState.GENERIC_ERROR

        return CardReaderOnboardingState.ONBOARDING_COMPLETED
    }

    private suspend fun isCountrySupported(): Boolean {
        return withContext(dispatchers.io) {
            wooStore.getStoreCountryCode(selectedSite.get())?.let { storeCountryCode ->
                SUPPORTED_COUNTRIES.any { it.equals(storeCountryCode, ignoreCase = true) }
            } ?: false.also { WooLog.e(WooLog.T.CARD_READER, "Store's country code not found.") }
        }
    }

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isWCPayInstalled(): Boolean = true

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isWCPayVersionSupported(): Boolean = true

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isWCPayActivated(): Boolean = true

    private fun isWCPaySetupCompleted(paymentAccount: WCPaymentAccountResult): Boolean =
        paymentAccount.status != WCPaymentAccountResult.WCPayAccountStatusEnum.NO_ACCOUNT

    // TODO cardreader Implement
    @Suppress("FunctionOnlyReturningConstant")
    private fun isWCPayInTestModeWithLiveStripeAccount(): Boolean = false

    private fun isStripeAccountUnderReview(paymentAccount: WCPaymentAccountResult): Boolean =
        paymentAccount.status == WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED &&
            !paymentAccount.hasPendingRequirements &&
            !paymentAccount.hasOverdueRequirements

    private fun isStripeAccountPendingRequirements(paymentAccount: WCPaymentAccountResult): Boolean =
        paymentAccount.status == WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED &&
            paymentAccount.hasPendingRequirements

    private fun isStripeAccountOverdueRequirements(paymentAccount: WCPaymentAccountResult): Boolean =
        paymentAccount.status == WCPaymentAccountResult.WCPayAccountStatusEnum.RESTRICTED &&
            paymentAccount.hasOverdueRequirements

    private fun isStripeAccountRejected(paymentAccount: WCPaymentAccountResult): Boolean =
        paymentAccount.status == WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_FRAUD ||
            paymentAccount.status == WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_LISTED ||
            paymentAccount.status == WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_TERMS_OF_SERVICE ||
            paymentAccount.status == WCPaymentAccountResult.WCPayAccountStatusEnum.REJECTED_OTHER
}

private fun isInUndefinedState(paymentAccount: WCPaymentAccountResult): Boolean =
    paymentAccount.status != WCPaymentAccountResult.WCPayAccountStatusEnum.COMPLETE

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
