package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel
import javax.inject.Inject

class ValidateHSTariffNumber @Inject constructor() {
    private val regex by lazy { Regex("""^(\d{1,2}\.?){3,6}$""") }

    operator fun invoke(
        tariffNumber: String,
        destinationCountryCode: String
    ): WooShippingCustomsFormViewModel.InputValue {
        val shouldValidateHSTariffNumber =
            shouldRequireHSTariffNumber(destinationCountryCode) || tariffNumber.isNotEmpty()
        if (!shouldValidateHSTariffNumber) return WooShippingCustomsFormViewModel.InputValue.Data(tariffNumber)

        val digits = tariffNumber.replace(Regex("""\D"""), "")

        val errorCode = when {
            tariffNumber.isEmpty() -> R.string.woo_shipping_labels_customs_product_details_value_required

            !regex.matches(tariffNumber) || digits.length !in 6..12 ->
                R.string.woo_shipping_labels_customs_product_details_tariff_invalid

            else -> null
        }

        return errorCode?.let {
            WooShippingCustomsFormViewModel.InputValue.Error(tariffNumber, it)
        } ?: WooShippingCustomsFormViewModel.InputValue.Data(tariffNumber)
    }

    private fun shouldRequireHSTariffNumber(destinationCountryCode: String) = destinationCountryCode in euUnionCountries

    companion object {
        private val euUnionCountries = listOf(
            "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR", "HR", "HU", "IE", "IT", "LT", "LU",
            "LV", "MT", "NL", "PL", "PT", "RO", "SE", "SI", "SK"
        )
    }
}
