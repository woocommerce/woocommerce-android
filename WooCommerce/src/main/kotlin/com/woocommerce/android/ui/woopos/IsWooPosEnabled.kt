package com.woocommerce.android.ui.woopos

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import com.woocommerce.android.util.IsWindowClassExpandedAndBigger
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
                isWindowSizeExpandedAndBigger()
            ).also { cachedResult = it }
    }
}
