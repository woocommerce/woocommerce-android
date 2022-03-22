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
    private val activePlugin: WCInPersonPaymentsStore.InPersonPaymentsPluginType?
        get() = appPrefs.getCardReaderPreferredPlugin(
            selectedSite.get().id,
            selectedSite.get().siteId,
            selectedSite.get().selfHostedSiteId
        )?.toInPersonPaymentsPluginType()
            ?: null.also { WooLog.e(WooLog.T.ORDERS, "Active Payment Plugin is not set") }

    suspend fun fetchCardDataUsedForOrderPayment(chargeId: String): CardDataUsedForOrderPaymentResult {
        val plugin = activePlugin
        return if (plugin == null) CardDataUsedForOrderPaymentResult.Error
        else {
            val result = ippStore.fetchPaymentCharge(plugin, selectedSite.get(), chargeId)
            if (result.isError) {
                WooLog.e(WooLog.T.ORDERS, "Could not fetch charge - ${result.error.message}")
                CardDataUsedForOrderPaymentResult.Error
            } else {
                val paymentMethodDetails = result.result?.paymentMethodDetails
                when (paymentMethodDetails?.type) {
                    "interac_present" -> {
                        CardDataUsedForOrderPaymentResult.Success(
                            cardBrand = paymentMethodDetails.interacCardDetails?.brand,
                            cardLast4 = paymentMethodDetails.interacCardDetails?.last4
                        )
                    }
                    "card_present" -> {
                        CardDataUsedForOrderPaymentResult.Success(
                            cardBrand = paymentMethodDetails.cardDetails?.brand,
                            cardLast4 = paymentMethodDetails.cardDetails?.last4
                        )
                    }
                    else ->
                        CardDataUsedForOrderPaymentResult.Success(
                            cardBrand = null,
                            cardLast4 = null
                        )
                }
            }
        }
    }

    sealed class CardDataUsedForOrderPaymentResult {
        object Error : CardDataUsedForOrderPaymentResult()
        data class Success(
            val cardBrand: String?,
            val cardLast4: String?
        ) : CardDataUsedForOrderPaymentResult()
    }
}
