package com.woocommerce.android.ui.refunds

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.cardreader.onboarding.PreferredPluginResult
import com.woocommerce.android.ui.cardreader.onboarding.toInPersonPaymentsPluginType
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import javax.inject.Inject

class PaymentChargeRepository @Inject constructor(
    private val ippStore: WCInPersonPaymentsStore,
    private val selectedSite: SelectedSite,
    private val appPrefs: AppPrefs,
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker,
) {
    suspend fun fetchCardDataUsedForOrderPayment(chargeId: String): CardDataUsedForOrderPaymentResult {
        val activePlugin = getActivePlugin()

        return if (activePlugin == null) {
            CardDataUsedForOrderPaymentResult.Error
        } else {
            val result = ippStore.fetchPaymentCharge(activePlugin, selectedSite.get(), chargeId)
            if (result.isError) {
                WooLog.e(WooLog.T.ORDERS, "Could not fetch charge - ${result.error.message}")
                CardDataUsedForOrderPaymentResult.Error
            } else {
                val paymentMethodDetails = result.result?.paymentMethodDetails
                when (paymentMethodDetails?.type) {
                    INTERAC_PRESENT -> {
                        CardDataUsedForOrderPaymentResult.Success(
                            cardBrand = paymentMethodDetails.interacCardDetails?.brand,
                            cardLast4 = paymentMethodDetails.interacCardDetails?.last4,
                            paymentMethodType = INTERAC_PRESENT
                        )
                    }
                    CARD_PRESENT -> {
                        CardDataUsedForOrderPaymentResult.Success(
                            cardBrand = paymentMethodDetails.cardDetails?.brand,
                            cardLast4 = paymentMethodDetails.cardDetails?.last4,
                            paymentMethodType = CARD_PRESENT
                        )
                    }
                    else ->
                        CardDataUsedForOrderPaymentResult.Success(
                            cardBrand = null,
                            cardLast4 = null,
                            paymentMethodType = null
                        )
                }
            }
        }
    }

    private suspend fun getActivePlugin() =
        appPrefs.getCardReaderPreferredPlugin(
            selectedSite.get().id,
            selectedSite.get().siteId,
            selectedSite.get().selfHostedSiteId
        )?.toInPersonPaymentsPluginType() ?: fetchPreferredPlugin()

    private suspend fun fetchPreferredPlugin() =
        when (val prefferedPluginResult = cardReaderOnboardingChecker.fetchPreferredPlugin()) {
            is PreferredPluginResult.Success -> prefferedPluginResult.type.toInPersonPaymentsPluginType()
            else -> null
        }

    sealed class CardDataUsedForOrderPaymentResult {
        object Error : CardDataUsedForOrderPaymentResult()
        data class Success(
            val cardBrand: String?,
            val cardLast4: String?,
            val paymentMethodType: String?
        ) : CardDataUsedForOrderPaymentResult()
    }

    companion object {
        private const val INTERAC_PRESENT = "interac_present"
        private const val CARD_PRESENT = "card_present"
    }
}
