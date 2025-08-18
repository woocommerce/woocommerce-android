package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import com.woocommerce.android.R
import javax.inject.Inject

class ValidateHSTariffNumber @Inject constructor() {
    private val regex by lazy { Regex("""^(\d{1,2}\.?){3,6}$""") }

    @Suppress("MagicNumber")
    operator fun invoke(
        tariffNumber: String,
        destinationCountryCode: String? = null
    ): WooShippingCustomsValidator.FieldValidationResult {
        val shouldValidateHSTariffNumber = shouldRequireHSTariffNumber(destinationCountryCode) ||
            tariffNumber.isNotEmpty()
        if (!shouldValidateHSTariffNumber) return WooShippingCustomsValidator.FieldValidationResult.Valid

        val digits = tariffNumber.replace(Regex("""\D"""), "")

        val errorCode = when {
            tariffNumber.isEmpty() -> R.string.woo_shipping_labels_customs_product_details_value_required

            !regex.matches(tariffNumber) || digits.length !in 6..12 ->
                R.string.woo_shipping_labels_customs_product_details_tariff_invalid

            else -> null
        }

        return errorCode?.let {
            WooShippingCustomsValidator.FieldValidationResult.Invalid(it)
        } ?: WooShippingCustomsValidator.FieldValidationResult.Valid
    }

    private fun shouldRequireHSTariffNumber(destinationCountryCode: String?) =
        destinationCountryCode in EU_UNION_COUNTRIES

    companion object {
        private val EU_UNION_COUNTRIES = listOf(
            "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR", "HR", "HU", "IE", "IT", "LT", "LU",
            "LV", "MT", "NL", "PL", "PT", "RO", "SE", "SI", "SK"
        )
    }
}
