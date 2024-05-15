package com.woocommerce.android.ui.woopos

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import com.woocommerce.android.util.IsWindowClassExpandedAndBigger
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS
import javax.inject.Inject

class IsWooPosEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val ippStore: WCInPersonPaymentsStore,
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin,
    private val isWindowSizeExpandedAndBigger: IsWindowClassExpandedAndBigger,
    private val isWooPosFFEnabled: IsWooPosFFEnabled,
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(): Boolean {
        val ippPlugin = getActivePaymentsPlugin() ?: return false
        val selectedSite = selectedSite.getOrNull() ?: return false
        val paymentAccount = ippStore.loadAccount(ippPlugin, selectedSite).model ?: return false
        val countryCode = paymentAccount.country

        return countryCode.lowercase() == "us" &&
            ippPlugin == WOOCOMMERCE_PAYMENTS &&
            paymentAccount.storeCurrencies.default.lowercase() == "usd" &&
            isPluginSetupCompleted(paymentAccount) &&
            isWindowSizeExpandedAndBigger() &&
            isWooPosFFEnabled()
    }

    private fun isPluginSetupCompleted(paymentAccount: WCPaymentAccountResult): Boolean =
        paymentAccount.status != WCPaymentAccountResult.WCPaymentAccountStatus.NO_ACCOUNT
}
