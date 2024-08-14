package com.woocommerce.android.ui.payments.cardreader.onboarding

import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.CARD_READER_ONBOARDING_COMPLETED
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.CARD_READER_ONBOARDING_NOT_COMPLETED
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.CARD_READER_ONBOARDING_PENDING
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.config.SupportedExtensionType
import com.woocommerce.android.cardreader.config.isExtensionSupported
import com.woocommerce.android.cardreader.config.minSupportedVersionForExtension
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.CashOnDeliverySettingsRepository
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.CashOnDeliveryDisabled
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.ChoosePaymentGatewayProvider
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.GenericError
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.NoConnectionError
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.OnboardingCompleted
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.PluginIsNotSupportedInTheCountry
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.PluginUnsupportedVersion
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.SetupNotCompleted
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StoreCountryNotSupported
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountCountryNotSupported
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountOverdueRequirement
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountPendingRequirement
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountRejected
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountUnderReview
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.WcpayNotActivated
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.WcpayNotInstalled
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.COMPLETE
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.ENABLED
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.NO_ACCOUNT
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_FRAUD
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_LISTED
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_OTHER
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_TERMS_OF_SERVICE
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED_SOON
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.PENDING_VERIFICATION
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

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
    private val cashOnDeliverySettingsRepository: CashOnDeliverySettingsRepository,
    private val cardReaderOnboardingCheckResultCache: CardReaderOnboardingCheckResultCache,
    private val paymentsFlowTracker: PaymentsFlowTracker,
) {
    suspend fun getOnboardingState(pluginType: PluginType? = null): CardReaderOnboardingState {
        val cachedValue = cardReaderOnboardingCheckResultCache.value

        return if (!networkStatus.isConnected()) {
            NoConnectionError
        } else if (cachedValue is CardReaderOnboardingCheckResultCache.Result.Cached) {
            cachedValue.state
        } else {
            fetchOnboardingState(pluginType)
                .also { state ->
                    val (status, version) = when (state) {
                        is OnboardingCompleted -> {
                            cardReaderOnboardingCheckResultCache.value =
                                CardReaderOnboardingCheckResultCache.Result.Cached(state)
                            CARD_READER_ONBOARDING_COMPLETED to state.version
                        }

                        is CashOnDeliveryDisabled -> CARD_READER_ONBOARDING_PENDING to state.version
                        is StripeAccountPendingRequirement -> CARD_READER_ONBOARDING_PENDING to state.version
                        else -> {
                            updatePluginExplicitlySelectedFlag(false)
                            CARD_READER_ONBOARDING_NOT_COMPLETED to null
                        }
                    }
                    updateSharedPreferences(
                        status,
                        state.preferredPlugin,
                        version
                    )
                }
        }.also {
            paymentsFlowTracker.trackOnboardingState(it)
        }
    }

    suspend fun fetchPreferredPlugin(): PreferredPluginResult {
        val fetchSitePluginsResult = wooStore.fetchSitePlugins(selectedSite.get())
        if (fetchSitePluginsResult.isError) {
            return PreferredPluginResult.Error
        }
        val wcPayPluginInfo = fetchSitePluginsResult.model.getPlugin(WOOCOMMERCE_PAYMENTS)
        val stripePluginInfo = fetchSitePluginsResult.model.getPlugin(STRIPE_EXTENSION_GATEWAY)
        return PreferredPluginResult.Success(getPreferredPlugin(stripePluginInfo, wcPayPluginInfo).type)
    }

    fun invalidateCache() {
        cardReaderOnboardingCheckResultCache.invalidate()
    }

    @Suppress("ReturnCount", "ComplexMethod", "LongMethod")
    private suspend fun fetchOnboardingState(pluginType: PluginType?): CardReaderOnboardingState {
        val countryCode = getStoreCountryCode()
        cardReaderTrackingInfoKeeper.setCountry(countryCode)
        val cardReaderConfig = cardReaderCountryConfigProvider.provideCountryConfigFor(countryCode)
        if (cardReaderConfig !is CardReaderConfigForSupportedCountry) {
            return StoreCountryNotSupported(countryCode)
        }

        val fetchSitePluginsResult = wooStore.fetchSitePlugins(selectedSite.get())
        if (fetchSitePluginsResult.isError) return GenericError
        val wcPayPluginInfo = fetchSitePluginsResult.model.getPlugin(WOOCOMMERCE_PAYMENTS)
        val stripePluginInfo = fetchSitePluginsResult.model.getPlugin(STRIPE_EXTENSION_GATEWAY)

        if (
            isBothPluginsActivated(wcPayPluginInfo, stripePluginInfo) &&
            isBothPluginsSupportedInTheCountry(cardReaderConfig)
        ) {
            when {
                isUserComingFromChoosePaymentGatewayScreen(pluginType) -> {
                    updateSharedPreferences(
                        CARD_READER_ONBOARDING_NOT_COMPLETED,
                        pluginType,
                        null
                    )
                    updatePluginExplicitlySelectedFlag(true)
                }
                !isPluginExplicitlySelected() -> {
                    return ChoosePaymentGatewayProvider
                }
            }
        } else {
            updatePluginExplicitlySelectedFlag(false)
        }

        val preferredPlugin = getUserSelectedPluginOrActivatedPlugin(wcPayPluginInfo, stripePluginInfo)

        if (!isPluginInstalled(preferredPlugin)) {
            when (preferredPlugin.type) {
                WOOCOMMERCE_PAYMENTS -> return WcpayNotInstalled
                STRIPE_EXTENSION_GATEWAY -> error("Developer error:`preferredPlugin` should be WCPay")
            }
        }

        if (!isPluginSupportedInCountry(preferredPlugin.type, cardReaderConfig)) {
            return PluginIsNotSupportedInTheCountry(preferredPlugin.type, countryCode!!)
        }

        if (!isPluginVersionSupported(
                preferredPlugin,
                getMinimumSupportedVersionForPlugin(preferredPlugin.type, cardReaderConfig)
            )
        ) {
            return PluginUnsupportedVersion(preferredPlugin.type)
        }

        if (!isPluginActivated(preferredPlugin.info)) {
            when (preferredPlugin.type) {
                WOOCOMMERCE_PAYMENTS -> return WcpayNotActivated
                STRIPE_EXTENSION_GATEWAY -> error("Developer error:`preferredPlugin` should be WCPay")
            }
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
        if (isPluginInTestModeWithLiveStripeAccount(paymentAccount)) {
            return PluginInTestModeWithLiveStripeAccount(
                preferredPlugin.type
            )
        }
        if (isStripeAccountUnderReview(paymentAccount)) return StripeAccountUnderReview(preferredPlugin.type)
        if (isStripeAccountOverdueRequirements(paymentAccount)) {
            return StripeAccountOverdueRequirement(
                preferredPlugin.type
            )
        }
        if (isStripeAccountPendingRequirements(paymentAccount)) {
            return StripeAccountPendingRequirement(
                paymentAccount.currentDeadline,
                preferredPlugin.type,
                preferredPlugin.info?.version,
                requireNotNull(countryCode)
            )
        }
        if (isStripeAccountRejected(paymentAccount)) return StripeAccountRejected(preferredPlugin.type)
        if (isInUndefinedState(paymentAccount)) return GenericError

        if (paymentAccount.status == PENDING_VERIFICATION) {
            return OnboardingCompleted(
                preferredPlugin.type,
                preferredPlugin.info?.version,
                requireNotNull(countryCode)
            )
        }

        if (
            !isCashOnDeliveryDisabledStateSkipped() &&
            !cashOnDeliverySettingsRepository.isCashOnDeliveryEnabled()
        ) {
            return CashOnDeliveryDisabled(
                requireNotNull(countryCode),
                preferredPlugin.type,
                preferredPlugin.info?.version
            )
        }

        return OnboardingCompleted(
            preferredPlugin.type,
            preferredPlugin.info?.version,
            requireNotNull(countryCode)
        )
    }

    private fun isCashOnDeliveryDisabledStateSkipped(): Boolean {
        val site = selectedSite.get()
        return appPrefsWrapper.isCashOnDeliveryDisabledStateSkipped(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId,
        )
    }

    private fun isUserComingFromChoosePaymentGatewayScreen(userSelectedPlugin: PluginType?): Boolean {
        if (userSelectedPlugin != null) {
            return true
        }
        return false
    }

    private fun getUserSelectedPluginOrActivatedPlugin(
        wcPayPluginInfo: SitePluginModel?,
        stripePluginInfo: SitePluginModel?,
    ): PluginWrapper {
        return when {
            isPluginExplicitlySelected() -> {
                val site = selectedSite.get()
                val pluginType = appPrefsWrapper.getCardReaderPreferredPlugin(
                    localSiteId = site.id,
                    remoteSiteId = site.siteId,
                    selfHostedSiteId = site.selfHostedSiteId,
                )
                pluginType?.let {
                    getUserSelectedPluginWrapper(it, wcPayPluginInfo, stripePluginInfo)
                } ?: run {
                    throw IllegalStateException(
                        "Developer Error: Plugin type cannot be null when the plugin explicitly selected flag is true"
                    )
                }
            }
            else -> {
                getPreferredPlugin(stripePluginInfo, wcPayPluginInfo)
            }
        }
    }

    private fun getUserSelectedPluginWrapper(
        userSelectedPlugin: PluginType,
        wcPayPluginInfo: SitePluginModel?,
        stripePluginInfo: SitePluginModel?,
    ): PluginWrapper {
        return PluginWrapper(userSelectedPlugin, userSelectedPlugin.getPluginInfo(wcPayPluginInfo, stripePluginInfo))
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

    private fun isBothPluginsSupportedInTheCountry(cardReaderConfig: CardReaderConfigForSupportedCountry) =
        isPluginSupportedInCountry(WOOCOMMERCE_PAYMENTS, cardReaderConfig) &&
            isPluginSupportedInCountry(STRIPE_EXTENSION_GATEWAY, cardReaderConfig)

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
        paymentAccount.status != COMPLETE && paymentAccount.status != ENABLED
            && paymentAccount.status != PENDING_VERIFICATION

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

    private fun updatePluginExplicitlySelectedFlag(isPluginExplicitlySelected: Boolean) {
        val site = selectedSite.get()
        appPrefsWrapper.setIsCardReaderPluginExplicitlySelectedFlag(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId,
            isPluginExplicitlySelected = isPluginExplicitlySelected
        )
    }

    private fun isPluginExplicitlySelected(): Boolean {
        val site = selectedSite.get()
        return appPrefsWrapper.isCardReaderPluginExplicitlySelected(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId,
        )
    }

    private fun List<SitePluginModel>?.getPlugin(type: PluginType) = this?.firstOrNull {
        it.name.endsWith(type.pluginName)
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

enum class PluginType(val pluginName: String) {
    WOOCOMMERCE_PAYMENTS("woocommerce-payments"),
    STRIPE_EXTENSION_GATEWAY("woocommerce-gateway-stripe")
}

fun PluginType.getPluginInfo(wcPayPluginInfo: SitePluginModel?, stripePluginInfo: SitePluginModel?) =
    when (this) {
        WOOCOMMERCE_PAYMENTS -> wcPayPluginInfo
        STRIPE_EXTENSION_GATEWAY -> stripePluginInfo
    }

private fun PluginType.toSupportedExtensionType() =
    when (this) {
        WOOCOMMERCE_PAYMENTS -> SupportedExtensionType.WC_PAY
        STRIPE_EXTENSION_GATEWAY -> SupportedExtensionType.STRIPE
    }

sealed class PreferredPluginResult {
    data class Success(val type: PluginType) : PreferredPluginResult()
    object Error : PreferredPluginResult()
}
