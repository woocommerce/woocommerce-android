package com.woocommerce.android.ui.orders.wooshippinglabels.address

import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class AddressValidationHelper @Inject constructor(
    private val resourceProvider: ResourceProvider
) {
    fun validateAtLeastOneOf(vararg values: String): String? {
        return if (values.all { it.isBlank() }) {
            resourceProvider.getString(R.string.woo_shipping_field_required_error)
        } else {
            null
        }
    }

    fun validateFieldRequired(value: String): String? {
        return if (value.isBlank()) {
            resourceProvider.getString(R.string.woo_shipping_field_required_error)
        } else {
            null
        }
    }

    fun validateEmail(value: String): String? {
        val errorResId = when {
            value.isBlank() -> R.string.woo_shipping_field_required_error
            !StringUtils.isValidEmail(value) -> R.string.email_invalid
            else -> null
        }
        return errorResId?.let { resourceProvider.getString(it) }
    }

    fun validatePhone(value: String, countryCode: String, isRequired: Boolean): String? {
        val digits = value.filter { it.isDigit() }
        val usPhoneLength = if (digits.startsWith('1')) US_PHONE_NUMBER_LENGTH + 1 else US_PHONE_NUMBER_LENGTH
        val errorResId = when {
            value.isBlank() -> if (isRequired) R.string.woo_shipping_field_required_error else null
            digits.isEmpty() || (countryCode == US_COUNTRY_CODE && digits.length != usPhoneLength) ->
                R.string.shipping_label_destination_address_phone_invalid
            else -> null
        }
        return errorResId?.let { resourceProvider.getString(it) }
    }

    fun isInternationalShipment(originCountryCode: String, destinationCountryCode: String) =
        originCountryCode.isNotBlank() && !originCountryCode.equals(destinationCountryCode, ignoreCase = true)

    fun isMissingOriginAddress(address: OriginShippingAddress) = with(address) {
        (address1.isNullOrBlank() && address2.isNullOrBlank()) || city.isNullOrBlank() || postcode.isBlank() ||
            (firstName.isNullOrBlank() && lastName.isNullOrBlank() && company.isNullOrBlank()) ||
            email.isNullOrBlank() || phone.isNullOrBlank() || country.isBlank()
    }

    fun isMissingDestinationAddress(address: Address) = with(address) {
        (address1.isBlank() && address2.isBlank()) || city.isBlank() || postcode.isBlank() ||
            (firstName.isBlank() && lastName.isBlank() && company.isBlank())
    }

    fun canFetchShippingRates(address: Address) = with(address) {
        city.isNotNullOrEmpty() && postcode.isNotNullOrEmpty() &&
            (firstName.isNotNullOrEmpty() || lastName.isNotNullOrEmpty() || company.isNotNullOrEmpty())
    }

    companion object {
        private const val US_PHONE_NUMBER_LENGTH = 10
        private const val US_COUNTRY_CODE = "US"
    }
}
