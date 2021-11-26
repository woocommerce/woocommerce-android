package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.cardreader.StripeExtensionFeatureFlag
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingState.*
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.pay.WCPaymentAccountResult
import org.wordpress.android.fluxc.model.pay.WCPaymentAccountResult.WCPayAccountStatusEnum.*
import org.wordpress.android.fluxc.persistence.WCPluginSqlUtils
import org.wordpress.android.fluxc.store.WCPayStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

private val SUPPORTED_COUNTRIES = listOf("US")

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val SUPPORTED_WCPAY_VERSION = "3.2.1"

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val SUPPORTED_STRIPE_TERMINAL_VERSION = "5.8.1"

class CardReaderOnboardingChecker @Inject constructor(
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val wooStore: WooCommerceStore,
    private val wcPayStore: WCPayStore,
    private val dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
    private val stripeExtensionFeatureFlag: StripeExtensionFeatureFlag,
) {
    suspend fun getOnboardingState(): CardReaderOnboardingState {
        if (!networkStatus.isConnected()) return NoConnectionError

        return fetchOnboardingState()
            .also {
                updateOnboardingCompletedFlag(isCompleted = it is OnboardingCompleted)
            }
    }

    @Suppress("ReturnCount", "ComplexMethod")
    private suspend fun fetchOnboardingState(): CardReaderOnboardingState {
        val countryCode = getStoreCountryCode()
        if (!isCountrySupported(countryCode)) return StoreCountryNotSupported(countryCode)

        val fetchSitePluginsResult = wooStore.fetchSitePlugins(selectedSite.get())
        if (fetchSitePluginsResult.isError) return GenericError
        val wcPayPluginInfo = wooStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_PAYMENTS)
        val stripePluginInfo = wooStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY)

        if (stripeExtensionFeatureFlag.isEnabled()) {
            if (isWCPayInstalled(wcPayPluginInfo) && isStripePluginInstalled(stripePluginInfo)) {
                // merchant has both the plugin installed!
                // Check if both the plugins are activated
                // if both are activated, then show popup to turn off any one
                if (
                    isStripeTerminalActivated(requireNotNull(stripePluginInfo)) &&
                    isWCPayActivated(requireNotNull(wcPayPluginInfo))
                ) {
                    return WcpayAndStripeActivated
                }
                if (!isStripeTerminalActivated(requireNotNull(stripePluginInfo))) {
                    if (!isWCPayVersionSupported(requireNotNull(wcPayPluginInfo))) return WcpayUnsupportedVersion
                    if (!isWCPayActivated(wcPayPluginInfo)) return WcpayNotActivated
                }
            } else if (isWCPayInstalled(wcPayPluginInfo)) {
                if (!isWCPayVersionSupported(requireNotNull(wcPayPluginInfo))) return WcpayUnsupportedVersion
                if (!isWCPayActivated(wcPayPluginInfo)) return WcpayNotActivated
            } else if (isStripePluginInstalled(stripePluginInfo)) {
                if (!isStripeTerminalActivated(requireNotNull(stripePluginInfo))) return StripeTerminal.NotActivated
                if (!isStripeTerminalVersionSupported(stripePluginInfo)) return StripeTerminal.UnsupportedVersion
            } else if (!isWCPayInstalled(wcPayPluginInfo)) {
                if (!isWCPayInstalled(wcPayPluginInfo)) return WcpayNotInstalled
            } else {
                if (!isStripePluginInstalled(stripePluginInfo)) return StripeTerminal.NotInstalled
            }
        } else {
            if (!isWCPayInstalled(wcPayPluginInfo)) return WcpayNotInstalled
            if (!isWCPayVersionSupported(requireNotNull(wcPayPluginInfo))) return WcpayUnsupportedVersion
            if (!isWCPayActivated(wcPayPluginInfo)) return WcpayNotActivated
        }

        val paymentAccount = wcPayStore.loadAccount(selectedSite.get()).model ?: return GenericError

        if (!isCountrySupported(paymentAccount.country)) return StripeAccountCountryNotSupported(paymentAccount.country)
        if (!isWCPaySetupCompleted(paymentAccount)) return WcpaySetupNotCompleted
        if (isWCPayInTestModeWithLiveStripeAccount(paymentAccount)) return WcpayInTestModeWithLiveStripeAccount
        if (isStripeAccountUnderReview(paymentAccount)) return StripeAccountUnderReview
        if (isStripeAccountOverdueRequirements(paymentAccount)) return StripeAccountOverdueRequirement
        if (isStripeAccountPendingRequirements(paymentAccount)) return StripeAccountPendingRequirement(
            paymentAccount.currentDeadline
        )
        if (isStripeAccountRejected(paymentAccount)) return StripeAccountRejected
        if (isInUndefinedState(paymentAccount)) return GenericError

        return OnboardingCompleted
    }

    private suspend fun getStoreCountryCode(): String? {
        return withContext(dispatchers.io) {
            wooStore.getStoreCountryCode(selectedSite.get()) ?: null.also {
                WooLog.e(WooLog.T.CARD_READER, "Store's country code not found.")
            }
        }
    }

    private fun isCountrySupported(countryCode: String?): Boolean {
        return countryCode?.let { storeCountryCode ->
            SUPPORTED_COUNTRIES.any { it.equals(storeCountryCode, ignoreCase = true) }
        } ?: false.also { WooLog.e(WooLog.T.CARD_READER, "Store's country code not found.") }
    }

    private fun isWCPayInstalled(pluginInfo: WCPluginSqlUtils.WCPluginModel?): Boolean = pluginInfo != null

    private fun isStripePluginInstalled(pluginInfo: WCPluginSqlUtils.WCPluginModel?): Boolean = pluginInfo != null

    private fun isWCPayVersionSupported(pluginInfo: WCPluginSqlUtils.WCPluginModel): Boolean =
        (pluginInfo.version).semverCompareTo(SUPPORTED_WCPAY_VERSION) >= 0

    private fun isStripeTerminalVersionSupported(pluginInfo: WCPluginSqlUtils.WCPluginModel): Boolean =
        (pluginInfo.version).semverCompareTo(SUPPORTED_STRIPE_TERMINAL_VERSION) >= 0

    private fun isWCPayActivated(pluginInfo: WCPluginSqlUtils.WCPluginModel): Boolean = pluginInfo.active

    private fun isStripeTerminalActivated(pluginInfo: WCPluginSqlUtils.WCPluginModel): Boolean = pluginInfo.active

    private fun isWCPaySetupCompleted(paymentAccount: WCPaymentAccountResult): Boolean =
        paymentAccount.status != NO_ACCOUNT

    private fun isWCPayInTestModeWithLiveStripeAccount(account: WCPaymentAccountResult): Boolean =
        account.testMode == true && account.isLive

    private fun isStripeAccountUnderReview(paymentAccount: WCPaymentAccountResult): Boolean =
        paymentAccount.status == RESTRICTED &&
            !paymentAccount.hasPendingRequirements &&
            !paymentAccount.hasOverdueRequirements

    private fun isStripeAccountPendingRequirements(paymentAccount: WCPaymentAccountResult): Boolean =
        (paymentAccount.status == RESTRICTED && paymentAccount.hasPendingRequirements) ||
            paymentAccount.status == RESTRICTED_SOON

    private fun isStripeAccountOverdueRequirements(paymentAccount: WCPaymentAccountResult): Boolean =
        paymentAccount.status == RESTRICTED &&
            paymentAccount.hasOverdueRequirements

    private fun isStripeAccountRejected(paymentAccount: WCPaymentAccountResult): Boolean =
        paymentAccount.status == REJECTED_FRAUD ||
            paymentAccount.status == REJECTED_LISTED ||
            paymentAccount.status == REJECTED_TERMS_OF_SERVICE ||
            paymentAccount.status == REJECTED_OTHER

    private fun isInUndefinedState(paymentAccount: WCPaymentAccountResult): Boolean =
        paymentAccount.status != COMPLETE

    private fun updateOnboardingCompletedFlag(isCompleted: Boolean) {
        val site = selectedSite.get()
        appPrefsWrapper.setCardReaderOnboardingCompleted(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId,
            isCompleted = isCompleted
        )
    }
}

sealed class CardReaderOnboardingState {
    object OnboardingCompleted : CardReaderOnboardingState()

    /**
     * Store is not located in one of the supported countries.
     */
    data class StoreCountryNotSupported(val countryCode: String?) : CardReaderOnboardingState()

    /**
     * WCPay plugin is not installed on the store.
     */
    object WcpayNotInstalled : CardReaderOnboardingState()

    sealed class StripeTerminal : CardReaderOnboardingState() {

        /**
         * stripe terminal plugin is not installed on the store.
         */
        object NotInstalled : CardReaderOnboardingState()

        /**
         * stripe terminal plugin is installed on the store, but the version is out-dated and doesn't
         * contain required APIs for card present payments.
         */
        object UnsupportedVersion : CardReaderOnboardingState()

        /**
         * stripe terminal is installed on the store but is not activated.
         */
        object NotActivated : CardReaderOnboardingState()

        /**
         * stripe terminal is installed and activated but requires to be setup first.
         */
        object SetupNotCompleted : CardReaderOnboardingState()
    }

    /**
     * WCPay plugin is installed on the store, but the version is out-dated and doesn't contain required APIs
     * for card present payments.
     */
    object WcpayUnsupportedVersion : CardReaderOnboardingState()

    /**
     * WCPay is installed on the store but is not activated.
     */
    object WcpayNotActivated : CardReaderOnboardingState()

    /**
     * WCPay is installed and activated but requires to be setup first.
     */
    object WcpaySetupNotCompleted : CardReaderOnboardingState()

    /**
     * The connected Stripe account has not been reviewed by Stripe yet. This is a temporary state and
     * the user needs to wait.
     */
    object WcpayAndStripeActivated : CardReaderOnboardingState()

    /**
     * This is a bit special case: WCPay is set to "dev mode" but the connected Stripe account is in live mode.
     * Connecting to a reader or accepting payments is not supported in this state.
     */
    object WcpayInTestModeWithLiveStripeAccount : CardReaderOnboardingState()

    /**
     * The connected Stripe account has not been reviewed by Stripe yet. This is a temporary state and
     * the user needs to wait.
     */
    object StripeAccountUnderReview : CardReaderOnboardingState()

    /**
     * There are some pending requirements on the connected Stripe account. The merchant still has some time before the
     * deadline to fix them expires. In-Person Payments should work without issues.
     */
    data class StripeAccountPendingRequirement(val dueDate: Long?) : CardReaderOnboardingState()

    /**
     * There are some overdue requirements on the connected Stripe account. Connecting to a reader or accepting
     * payments is not supported in this state.
     */
    object StripeAccountOverdueRequirement : CardReaderOnboardingState()

    /**
     * The Stripe account was rejected by Stripe. This can happen for example when the account is flagged as fraudulent
     * or the merchant violates the terms of service
     */
    object StripeAccountRejected : CardReaderOnboardingState()

    /**
     * The Stripe account is attached to an address in one of the unsupported countries.
     */
    data class StripeAccountCountryNotSupported(val countryCode: String?) : CardReaderOnboardingState()

    /**
     * Generic error - for example, one of the requests failed.
     */
    object GenericError : CardReaderOnboardingState()

    /**
     * Internet connection is not available.
     */
    object NoConnectionError : CardReaderOnboardingState()
}
