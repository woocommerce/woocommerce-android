package com.woocommerce.android.ui.woopos

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.IsWindowClassExpandedAndBigger
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsWooPosEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val ippStore: WCInPersonPaymentsStore,
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin,
    private val isWindowSizeExpandedAndBigger: IsWindowClassExpandedAndBigger,
    private val isWooPosFFEnabled: IsWooPosFFEnabled,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
) {
    private var cachedResult: Boolean? = null

    @Suppress("ReturnCount")
    suspend operator fun invoke(): Boolean {
        cachedResult?.let { return it }

        if (!isWooPosFFEnabled()) return false

        val selectedSite = selectedSite.getOrNull() ?: return false
        val ippPlugin = getActivePaymentsPlugin() ?: return false
        val paymentAccount = ippStore.loadAccount(ippPlugin, selectedSite).model ?: return false
        val countryCode = paymentAccount.country

        return (
            countryCode.lowercase() == "us" &&
                ippPlugin == WOOCOMMERCE_PAYMENTS &&
                paymentAccount.storeCurrencies.default.lowercase() == "usd" &&
                isWindowSizeExpandedAndBigger() &&
                isPluginSetupEnabled(paymentAccount) &&
                isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps()
            ).also { cachedResult = it }
    }

    private fun isPluginSetupEnabled(paymentAccount: WCPaymentAccountResult): Boolean =
        paymentAccount.status == WCPaymentAccountResult.WCPaymentAccountStatus.COMPLETE ||
            paymentAccount.status == WCPaymentAccountResult.WCPaymentAccountStatus.ENABLED

    private fun isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps(): Boolean {
        val wooCoreVersion = getWooCoreVersion() ?: return false
        return wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_ORDER_AUTO_DRAFTS_AND_EXTRA_PAYMENTS_PROPS) >= 0
    }


    private companion object {
        const val WC_VERSION_SUPPORTS_ORDER_AUTO_DRAFTS_AND_EXTRA_PAYMENTS_PROPS = "6.6.0"
    }
}
