package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingState.*
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.pay.WCPaymentAccountResult
import org.wordpress.android.fluxc.persistence.WCPluginSqlUtils
import org.wordpress.android.fluxc.store.WCPayStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

private val SUPPORTED_COUNTRIES = listOf("US")

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val SUPPORTED_WCPAY_VERSION = "2.5.0"

@Suppress("TooManyFunctions")
class CardReaderOnboardingChecker @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooStore: WooCommerceStore,
    private val wcPayStore: WCPayStore,
    private val dispatchers: CoroutineDispatchers
) {
    @Suppress("ReturnCount")
    suspend fun getOnboardingState(): CardReaderOnboardingState {
        if (!isCountrySupported()) return COUNTRY_NOT_SUPPORTED

        val fetchSitePluginsResult = wooStore.fetchSitePlugins(selectedSite.get())
        if (fetchSitePluginsResult.isError) return GENERIC_ERROR
        val pluginInfo = wooStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_PAYMENTS)

        if (!isWCPayInstalled(pluginInfo)) return WCPAY_NOT_INSTALLED
        if (!isWCPayVersionSupported(requireNotNull(pluginInfo))) return WCPAY_UNSUPPORTED_VERSION
        if (!isWCPayActivated(pluginInfo)) return WCPAY_NOT_ACTIVATED

        val paymentAccount = wcPayStore.loadAccount(selectedSite.get()).model ?: return GENERIC_ERROR

        if (!isWCPaySetupCompleted(paymentAccount)) return WCPAY_SETUP_NOT_COMPLETED
        if (isWCPayInTestModeWithLiveStripeAccount()) return WCPAY_IN_TEST_MODE_WITH_LIVE_STRIPE_ACCOUNT
        if (isStripeAccountUnderReview(paymentAccount)) return STRIPE_ACCOUNT_UNDER_REVIEW
        if (isStripeAccountPendingRequirements(paymentAccount)) return STRIPE_ACCOUNT_PENDING_REQUIREMENT
        if (isStripeAccountOverdueRequirements(paymentAccount)) return STRIPE_ACCOUNT_OVERDUE_REQUIREMENT
        if (isStripeAccountRejected(paymentAccount)) return STRIPE_ACCOUNT_REJECTED
        if (isInUndefinedState(paymentAccount)) return GENERIC_ERROR

        return ONBOARDING_COMPLETED
    }

    private suspend fun isCountrySupported(): Boolean {
        return withContext(dispatchers.io) {
            wooStore.getStoreCountryCode(selectedSite.get())?.let { storeCountryCode ->
                SUPPORTED_COUNTRIES.any { it.equals(storeCountryCode, ignoreCase = true) }
            } ?: false.also { WooLog.e(WooLog.T.CARD_READER, "Store's country code not found.") }
        }
    }

    private fun isWCPayInstalled(pluginInfo: WCPluginSqlUtils.WCPluginModel?): Boolean = pluginInfo != null

    private fun isWCPayVersionSupported(pluginInfo: WCPluginSqlUtils.WCPluginModel): Boolean =
        (pluginInfo.version).semverCompareTo(SUPPORTED_WCPAY_VERSION) >= 0

    private fun isWCPayActivated(pluginInfo: WCPluginSqlUtils.WCPluginModel): Boolean = pluginInfo.active

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
