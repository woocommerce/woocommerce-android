package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsItem
import java.math.BigDecimal
import javax.inject.Inject

class ValidateITN @Inject constructor(
    private val validateHSTariffNumber: ValidateHSTariffNumber
) {
    companion object {
        private val itnExemptCountries = listOf("CA")
        private val itnRequiredCountries = listOf("IR", "KP", "SY", "CU", "SD")

        private const val MAX_SHIPPING_ITEM_VALUE_FOR_CUSTOMS = 2500

        /**
         * For information regarding the format of the ITN, check the Appendix A of
         * [Export Compliance Customs Data Requirements](https://postalpro.usps.com/node/3973)
         */
        private const val ITN_REGEX_STRING =
            """^(?:AES X\d{14}|NOEEI 30\.\d{1,2}(?:\([a-z]\)(?:\(\d\))?)?)${'$'}"""
    }

    private val List<CustomsItem>.totalValue: BigDecimal
        get() = this.sumOf { it.totalValue }

    private val CustomsData.classesAbove2500: Map<String, List<CustomsItem>>
        get() = this.items.filter {
            it.hsTariffNumber.isNotEmpty() && validateHSTariffNumber(it.hsTariffNumber).isValid
        }
            .groupBy { it.hsTariffNumber }
            .filter { it.value.totalValue > MAX_SHIPPING_ITEM_VALUE_FOR_CUSTOMS.toBigDecimal() }

    operator fun invoke(
        customsData: CustomsData,
        destinationCountry: String
    ): ITNValidationResult {
        val itn = customsData.itn.trim()

        return when {
            itn.isNotEmpty() -> {
                if (itn.matches(ITN_REGEX_STRING.toRegex())) {
                    ITNValidationResult.Valid
                } else {
                    ITNValidationResult.InvalidFormat
                }
            }

            itnExemptCountries.contains(destinationCountry) -> ITNValidationResult.Valid

            customsData.items.totalValue > MAX_SHIPPING_ITEM_VALUE_FOR_CUSTOMS.toBigDecimal() &&
                customsData.items.none { it.hsTariffNumber.isNotEmpty() } ->
                ITNValidationResult.Missing(ITNMissingCause.TotalValue)

            customsData.classesAbove2500.isNotEmpty() ->
                ITNValidationResult.Missing(ITNMissingCause.HSTariffValue(customsData.classesAbove2500.keys.first()))

            itnRequiredCountries.contains(destinationCountry) ->
                ITNValidationResult.Missing(ITNMissingCause.DestinationCountry)

            else -> ITNValidationResult.Valid
        }
    }

    sealed interface ITNValidationResult {
        data object Valid : ITNValidationResult
        data class Missing(val cause: ITNMissingCause) : ITNValidationResult
        data object InvalidFormat : ITNValidationResult
    }

    sealed interface ITNMissingCause {
        data object TotalValue : ITNMissingCause
        data class HSTariffValue(val hsTariffNumber: String) : ITNMissingCause
        data object DestinationCountry : ITNMissingCause
    }
}
