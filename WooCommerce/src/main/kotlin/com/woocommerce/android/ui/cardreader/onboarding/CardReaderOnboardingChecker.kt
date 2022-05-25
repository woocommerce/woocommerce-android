package com.woocommerce.android.ui.cardreader.onboarding

import android.os.Parcelable
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.CARD_READER_ONBOARDING_COMPLETED
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.CARD_READER_ONBOARDING_NOT_COMPLETED
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.CARD_READER_ONBOARDING_PENDING
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.internal.config.SupportedExtensionType
import com.woocommerce.android.cardreader.internal.config.isExtensionSupported
import com.woocommerce.android.cardreader.internal.config.minSupportedVersionForExtension
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.cardreader.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.GenericError
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.NoConnectionError
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.OnboardingCompleted
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.PluginIsNotSupportedInTheCountry
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.PluginUnsupportedVersion
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.SetupNotCompleted
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.StoreCountryNotSupported
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.StripeAccountCountryNotSupported
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.StripeAccountOverdueRequirement
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.StripeAccountPendingRequirement
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.StripeAccountRejected
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.StripeAccountUnderReview
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.WcpayAndStripeActivated
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.WcpayNotActivated
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingState.WcpayNotInstalled
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.COMPLETE
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.NO_ACCOUNT
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_FRAUD
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_LISTED
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_OTHER
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_TERMS_OF_SERVICE
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED_SOON
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

const val WCPAY_RECEIPTS_SENDING_SUPPORT_VERSION = "4.0.0"

/**
 * This class is used to check if the selected store is ready to accept In Person Payments. The app should check store's
 * eligibility every time it attempts to connect to a card reader.
 *
 * This class contains a side-effect, it stores "onboarding completed"/"onboarding not completed"/"onboarding pending"
 * and Preferred Plugin (either WCPay or Stripe Extension) into shared preferences.
 *
 * Onboarding Pending means that the store is ready to accept in person payments, but the Stripe account contains some
 * pending requirements and will be disabled if the requirements are not met.
 */
class CardReaderOnboardingChecker @Inject constructor(
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val wooStore: WooCommerceStore,
    private val inPersonPaymentsStore: WCInPersonPaymentsStore,
    private val dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
    private val cardReaderTrackingInfoKeeper: CardReaderTrackingInfoKeeper,
    private val cardReaderCountryConfigProvider: CardReaderCountryConfigProvider,
) {
    suspend fun getOnboardingState(): CardReaderOnboardingState {
        if (!networkStatus.isConnected()) return NoConnectionError

        return fetchOnboardingState()
            .also {
                val (status, version) = when (it) {
                    is OnboardingCompleted -> CARD_READER_ONBOARDING_COMPLETED to it.version
                    is StripeAccountPendingRequirement -> CARD_READER_ONBOARDING_PENDING to it.version
                    else -> CARD_READER_ONBOARDING_NOT_COMPLETED to null
                }
                updateSharedPreferences(
                    status,
                    it.preferredPlugin,
                    version
                )
            }
    }

    suspend fun fetchPreferredPlugin(): PreferredPluginResult {
        val fetchSitePluginsResult = wooStore.fetchSitePlugins(selectedSite.get())
        if (fetchSitePluginsResult.isError) {
            return PreferredPluginResult.Error
        }
        val wcPayPluginInfo = wooStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_PAYMENTS)
        val stripePluginInfo = wooStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY)
        return PreferredPluginResult.Success(getPreferredPlugin(stripePluginInfo, wcPayPluginInfo).type)
    }

    @Suppress("ReturnCount", "ComplexMethod", "LongMethod")
    private suspend fun fetchOnboardingState(): CardReaderOnboardingState {
        val countryCode = getStoreCountryCode()
        cardReaderTrackingInfoKeeper.setCountry(countryCode)
        val cardReaderConfig = cardReaderCountryConfigProvider.provideCountryConfigFor(countryCode)
        if (cardReaderConfig !is CardReaderConfigForSupportedCountry)
            return StoreCountryNotSupported(countryCode)

        val fetchSitePluginsResult = wooStore.fetchSitePlugins(selectedSite.get())
        if (fetchSitePluginsResult.isError) return GenericError
        val wcPayPluginInfo = wooStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_PAYMENTS)
        val stripePluginInfo = wooStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY)

        if (isBothPluginsActivated(wcPayPluginInfo, stripePluginInfo)) {
            PluginType.values().forEach { pluginType ->
                if (!isPluginSupportedInCountry(pluginType, cardReaderConfig)) {
                    return PluginIsNotSupportedInTheCountry(pluginType, countryCode!!)
                }
            }
            return WcpayAndStripeActivated
        }

        val preferredPlugin = getPreferredPlugin(stripePluginInfo, wcPayPluginInfo)

        if (!isPluginInstalled(preferredPlugin)) when (preferredPlugin.type) {
            WOOCOMMERCE_PAYMENTS -> return WcpayNotInstalled
            STRIPE_EXTENSION_GATEWAY -> throw IllegalStateException("Developer error:`preferredPlugin` should be WCPay")
        }

        if (!isPluginSupportedInCountry(preferredPlugin.type, cardReaderConfig))
            return PluginIsNotSupportedInTheCountry(preferredPlugin.type, countryCode!!)

        if (!isPluginVersionSupported(
                preferredPlugin,
                getMinimumSupportedVersionForPlugin(preferredPlugin.type, cardReaderConfig)
            )
        ) return PluginUnsupportedVersion(preferredPlugin.type)

        if (!isPluginActivated(preferredPlugin.info)) when (preferredPlugin.type) {
            WOOCOMMERCE_PAYMENTS -> return WcpayNotActivated
            STRIPE_EXTENSION_GATEWAY -> throw IllegalStateException("Developer error:`preferredPlugin` should be WCPay")
        }

        val fluxCPluginType = preferredPlugin.type.toInPersonPaymentsPluginType()

        val paymentAccount =
            inPersonPaymentsStore.loadAccount(fluxCPluginType, selectedSite.get()).model ?: return GenericError

        saveStatementDescriptor(paymentAccount.statementDescriptor)

        val countryConfigOfStripe = cardReaderCountryConfigProvider.provideCountryConfigFor(paymentAccount.country)
        if (countryConfigOfStripe !is CardReaderConfigForSupportedCountry) {
            return StripeAccountCountryNotSupported(
                preferredPlugin.type,
                paymentAccount.country
            )
        }
        if (!isPluginSetupCompleted(paymentAccount)) return SetupNotCompleted(preferredPlugin.type)
        if (isPluginInTestModeWithLiveStripeAccount(paymentAccount)) return PluginInTestModeWithLiveStripeAccount(
            preferredPlugin.type
        )
        if (isStripeAccountUnderReview(paymentAccount)) return StripeAccountUnderReview(preferredPlugin.type)
        if (isStripeAccountOverdueRequirements(paymentAccount)) return StripeAccountOverdueRequirement(
            preferredPlugin.type
        )
        if (isStripeAccountPendingRequirements(paymentAccount)) return StripeAccountPendingRequirement(
            paymentAccount.currentDeadline,
            preferredPlugin.type,
            preferredPlugin.info?.version,
            requireNotNull(countryCode)
        )
        if (isStripeAccountRejected(paymentAccount)) return StripeAccountRejected(preferredPlugin.type)
        if (isInUndefinedState(paymentAccount)) return GenericError

        return OnboardingCompleted(
            preferredPlugin.type,
            preferredPlugin.info?.version,
            requireNotNull(countryCode)
        )
    }

    private fun isPluginSupportedInCountry(
        pluginType: PluginType,
        cardReaderConfig: CardReaderConfigForSupportedCountry
    ) = cardReaderConfig.isExtensionSupported(pluginType.toSupportedExtensionType())

    private fun getMinimumSupportedVersionForPlugin(
        preferredPluginType: PluginType,
        cardReaderConfig: CardReaderConfigForSupportedCountry
    ) = cardReaderConfig.minSupportedVersionForExtension(preferredPluginType.toSupportedExtensionType())

    private fun saveStatementDescriptor(statementDescriptor: String?) {
        val site = selectedSite.get()
        appPrefsWrapper.setCardReaderStatementDescriptor(
            statementDescriptor = statementDescriptor,
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId,
        )
    }

    private fun isBothPluginsActivated(
        wcPayPluginInfo: SitePluginModel?,
        stripePluginInfo: SitePluginModel?
    ) = isPluginActivated(wcPayPluginInfo) && isPluginActivated(stripePluginInfo)

    private fun getPreferredPlugin(
        stripePluginInfo: SitePluginModel?,
        wcPayPluginInfo: SitePluginModel?,
    ): PluginWrapper = if (isPluginActivated(stripePluginInfo) && !isPluginActivated(wcPayPluginInfo)) {
        PluginWrapper(
            STRIPE_EXTENSION_GATEWAY,
            stripePluginInfo,
        )
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

    private fun isPluginInstalled(plugin: PluginWrapper): Boolean {
        return plugin.info != null
    }

    private fun isPluginVersionSupported(
        plugin: PluginWrapper,
        minimumSupportedVersion: String
    ): Boolean {
        return plugin.info != null && (plugin.info.version).semverCompareTo(minimumSupportedVersion) >= 0
    }

    private fun isPluginActivated(pluginInfo: SitePluginModel?): Boolean = pluginInfo?.isActive == true

    private fun isPluginSetupCompleted(paymentAccount: WCPaymentAccountResult): Boolean =
        paymentAccount.status != NO_ACCOUNT

    private fun isPluginInTestModeWithLiveStripeAccount(account: WCPaymentAccountResult): Boolean =
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

    private fun updateSharedPreferences(
        status: CardReaderOnboardingStatus,
        preferredPlugin: PluginType?,
        version: String?,
    ) {
        val site = selectedSite.get()
        appPrefsWrapper.setCardReaderOnboardingData(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId,
            PersistentOnboardingData(status, preferredPlugin, version),
        )
    }
}

data class PersistentOnboardingData(
    val status: CardReaderOnboardingStatus,
    val preferredPlugin: PluginType?,
    val version: String?,
)

fun PluginType.toInPersonPaymentsPluginType(): InPersonPaymentsPluginType = when (this) {
    WOOCOMMERCE_PAYMENTS -> InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS
    STRIPE_EXTENSION_GATEWAY -> InPersonPaymentsPluginType.STRIPE
}

private data class PluginWrapper(
    val type: PluginType,
    val info: SitePluginModel?
)

enum class PluginType {
    WOOCOMMERCE_PAYMENTS,
    STRIPE_EXTENSION_GATEWAY
}

private fun PluginType.toSupportedExtensionType() =
    when (this) {
        WOOCOMMERCE_PAYMENTS -> SupportedExtensionType.WC_PAY
        STRIPE_EXTENSION_GATEWAY -> SupportedExtensionType.STRIPE
    }

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
    object WcpayNotInstalled : CardReaderOnboardingState(preferredPlugin = WOOCOMMERCE_PAYMENTS)

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
    object WcpayNotActivated : CardReaderOnboardingState(preferredPlugin = WOOCOMMERCE_PAYMENTS)

    /**
     * Plugin is installed and activated but requires to be setup first.
     */
    @Parcelize
    data class SetupNotCompleted(override val preferredPlugin: PluginType) : CardReaderOnboardingState()

    /**
     * Both plugins are installed and activated on the site. IPP are not supported in this state.
     */
    @Parcelize
    object WcpayAndStripeActivated : CardReaderOnboardingState()

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
}

sealed class PreferredPluginResult {
    data class Success(val type: PluginType) : PreferredPluginResult()
    object Error : PreferredPluginResult()
}
