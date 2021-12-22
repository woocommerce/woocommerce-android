package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.cardreader.StripeExtensionFeatureFlag
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderOnboardingState.*
import com.woocommerce.android.ui.prefs.cardreader.onboarding.PluginType.STRIPE_TERMINAL_GATEWAY
import com.woocommerce.android.ui.prefs.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.*
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject
import org.wordpress.android.fluxc.persistence.WCPluginSqlUtils.WCPluginModel
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType

private val SUPPORTED_COUNTRIES = listOf("US")

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val SUPPORTED_WCPAY_VERSION = "3.2.1"

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val SUPPORTED_STRIPE_EXTENSION_VERSION = "5.8.1"

class CardReaderOnboardingChecker @Inject constructor(
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val wooStore: WooCommerceStore,
    private val inPersonPaymentsStore: WCInPersonPaymentsStore,
    private val dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
    private val stripeExtensionFeatureFlag: StripeExtensionFeatureFlag,
) {
    suspend fun getOnboardingState(): CardReaderOnboardingState {
        if (!networkStatus.isConnected()) return NoConnectionError

        return fetchOnboardingState()
            .also {
                when (it) {
                    is OnboardingCompleted -> updateOnboardingCompletedStatus(it.pluginType)
                    else -> updateOnboardingCompletedStatus(null)
                }
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

        if (isBothPluginsActivated(wcPayPluginInfo, stripePluginInfo)) return WcpayAndStripeActivated

        val preferredPlugin = getPreferredPlugin(stripePluginInfo, wcPayPluginInfo)

        if (!isPluginInstalled(preferredPlugin)) when (preferredPlugin.type) {
            WOOCOMMERCE_PAYMENTS -> return WcpayNotInstalled
            STRIPE_TERMINAL_GATEWAY -> throw IllegalStateException("Developer error: `preferredPlugin` should be WCPay")
        }

        if (!isPluginVersionSupported(preferredPlugin)) return PluginUnsupportedVersion(preferredPlugin.type)

        if (!isPluginActivated(preferredPlugin.info)) when (preferredPlugin.type) {
            WOOCOMMERCE_PAYMENTS -> return WcpayNotActivated
            STRIPE_TERMINAL_GATEWAY -> throw IllegalStateException("Developer error: `preferredPlugin` should be WCPay")
        }

        val fluxCPluginType = preferredPlugin.type.toInPersonPaymentsPluginType()

        val paymentAccount =
            inPersonPaymentsStore.loadAccount(fluxCPluginType, selectedSite.get()).model ?: return GenericError

        if (!isCountrySupported(paymentAccount.country)) return StripeAccountCountryNotSupported(paymentAccount.country)
        if (!isPluginSetupCompleted(paymentAccount)) return SetupNotCompleted(preferredPlugin.type)
        if (isWCPayInTestModeWithLiveStripeAccount(paymentAccount)) return WcpayInTestModeWithLiveStripeAccount
        if (isStripeAccountUnderReview(paymentAccount)) return StripeAccountUnderReview
        if (isStripeAccountOverdueRequirements(paymentAccount)) return StripeAccountOverdueRequirement
        if (isStripeAccountPendingRequirements(paymentAccount)) return StripeAccountPendingRequirement(
            paymentAccount.currentDeadline
        )
        if (isStripeAccountRejected(paymentAccount)) return StripeAccountRejected
        if (isInUndefinedState(paymentAccount)) return GenericError

        return OnboardingCompleted(preferredPlugin.type)
    }

    private fun isBothPluginsActivated(
        wcPayPluginInfo: WCPluginModel?,
        stripePluginInfo: WCPluginModel?
    ) = stripeExtensionFeatureFlag.isEnabled() &&
        isPluginActivated(wcPayPluginInfo) &&
        isPluginActivated(stripePluginInfo)

    private fun getPreferredPlugin(stripePluginInfo: WCPluginModel?, wcPayPluginInfo: WCPluginModel?): PluginWrapper =
        if (stripeExtensionFeatureFlag.isEnabled() &&
            isPluginActivated(stripePluginInfo) &&
            !isPluginActivated(wcPayPluginInfo)
        ) {
            PluginWrapper(STRIPE_TERMINAL_GATEWAY, stripePluginInfo)
        } else {
            // Default to WCPay when Stripe Extension is not active
            PluginWrapper(WOOCOMMERCE_PAYMENTS, wcPayPluginInfo)
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

    private fun isPluginInstalled(plugin: PluginWrapper): Boolean {
        return plugin.info != null
    }

    private fun isPluginVersionSupported(plugin: PluginWrapper): Boolean =
        plugin.info != null && (plugin.info.version).semverCompareTo(plugin.type.minSupportedVersion) >= 0

    private fun isPluginActivated(pluginInfo: WCPluginModel?): Boolean = pluginInfo?.active == true

    private fun isPluginSetupCompleted(paymentAccount: WCPaymentAccountResult): Boolean =
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

    private fun updateOnboardingCompletedStatus(pluginType: PluginType?) {
        val site = selectedSite.get()
        appPrefsWrapper.setCardReaderOnboardingCompleted(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId,
            pluginType
        )
    }
}

fun PluginType.toInPersonPaymentsPluginType(): InPersonPaymentsPluginType = when (this) {
    WOOCOMMERCE_PAYMENTS -> InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS
    STRIPE_TERMINAL_GATEWAY -> InPersonPaymentsPluginType.STRIPE
}

private data class PluginWrapper(val type: PluginType, val info: WCPluginModel?)

enum class PluginType(val minSupportedVersion: String) {
    WOOCOMMERCE_PAYMENTS(SUPPORTED_WCPAY_VERSION),
    STRIPE_TERMINAL_GATEWAY(SUPPORTED_STRIPE_EXTENSION_VERSION)
}

sealed class CardReaderOnboardingState {
    data class OnboardingCompleted(val pluginType: PluginType) : CardReaderOnboardingState()

    /**
     * Store is not located in one of the supported countries.
     */
    data class StoreCountryNotSupported(val countryCode: String?) : CardReaderOnboardingState()

    /**
     * WCPay plugin is not installed on the store.
     */
    object WcpayNotInstalled : CardReaderOnboardingState()

    /**
     * Plugin is installed on the store, but the version is out-dated and doesn't contain required APIs
     * for card present payments.
     */
    data class PluginUnsupportedVersion(val pluginType: PluginType) : CardReaderOnboardingState()

    /**
     * WCPay is installed on the store but is not activated.
     */
    object WcpayNotActivated : CardReaderOnboardingState()

    /**
     * Plugin is installed and activated but requires to be setup first.
     */
    data class SetupNotCompleted(val pluginType: PluginType) : CardReaderOnboardingState()

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
