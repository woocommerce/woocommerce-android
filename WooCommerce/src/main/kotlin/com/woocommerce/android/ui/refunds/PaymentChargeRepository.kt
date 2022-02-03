package com.woocommerce.android.ui.refunds

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.cardreader.onboarding.toInPersonPaymentsPluginType
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import javax.inject.Inject

class PaymentChargeRepository @Inject constructor(
    private val ippStore: WCInPersonPaymentsStore,
    private val selectedSite: SelectedSite,
    private val appPrefs: AppPrefs
) {

    private val activePlugin: WCInPersonPaymentsStore.InPersonPaymentsPluginType by lazy {
        appPrefs.getPaymentPluginType(
            selectedSite.get().id,
            selectedSite.get().siteId,
            selectedSite.get().selfHostedSiteId
        ).toInPersonPaymentsPluginType()
    }

    suspend fun fetchCardDataUsedForOrderPayment(chargeId: String): CardDataUsedForOrderPaymentResult {
        val result = ippStore.fetchPaymentCharge(activePlugin, selectedSite.get(), chargeId)
        return if (result.isError) {
            WooLog.e(WooLog.T.ORDERS, "Could not fetch charge - ${result.error.message}")
            CardDataUsedForOrderPaymentResult.Error
        } else {
            CardDataUsedForOrderPaymentResult.Success(
                brand = result.result?.paymentMethodDetails?.cardPresent?.brand,
                cardLast4 = result.result?.paymentMethodDetails?.cardPresent?.last4
            )
        }
    }

    sealed class CardDataUsedForOrderPaymentResult {
        object Error : CardDataUsedForOrderPaymentResult()
        data class Success(
            val brand: String?,
            val cardLast4: String?
        ) : CardDataUsedForOrderPaymentResult()
    }
}
