package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

class TerminalPaymentPreparationResolver(
    private val wooStore: WooCommerceStore,
    private val appPrefs: AppPrefs = AppPrefs,
) {
    suspend fun resolve(
        countryCode: String,
        site: SiteModel,
        onRouteCheckFailed: () -> Unit,
    ): PaymentInfo.TerminalPaymentPreparation {
        val isWooPaymentsPreferred = appPrefs.getCardReaderPreferredPlugin(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId,
        ) == PluginType.WOOCOMMERCE_PAYMENTS
        val isRouteAvailableInCanada = countryCode.equals(CANADA_COUNTRY_CODE, ignoreCase = true) &&
            isWooPaymentsPreferred &&
            isTerminalPaymentPreparationRouteAvailable(site, onRouteCheckFailed)
        return TerminalPaymentIntentConfig.terminalPaymentPreparation(
            countryCode = countryCode,
            isWooPaymentsPreferred = isWooPaymentsPreferred,
            isTerminalPaymentPreparationRouteAvailableInCanada = isRouteAvailableInCanada,
        )
    }

    private suspend fun isTerminalPaymentPreparationRouteAvailable(
        site: SiteModel,
        onRouteCheckFailed: () -> Unit,
    ): Boolean = wooStore.fetchSiteRootApiRoutes(site).let { result ->
        if (result.isError) {
            onRouteCheckFailed()
            false
        } else {
            TerminalPaymentIntentConfig.hasTerminalPaymentPreparationRoute(result.model.orEmpty())
        }
    }

    private companion object {
        const val CANADA_COUNTRY_CODE = "CA"
    }
}
