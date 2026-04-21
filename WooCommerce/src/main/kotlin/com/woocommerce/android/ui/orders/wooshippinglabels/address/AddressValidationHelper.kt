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

    fun validateUSCustomsPhone(value: String): String? {
        return when {
            value.isBlank() -> resourceProvider.getString(R.string.woo_shipping_field_required_error)
            value.replace(Regex("^1|[^\\d]"), "").length != US_PHONE_NUMBER_LENGTH -> {
                resourceProvider.getString(R.string.shipping_label_destination_address_phone_invalid)
            }

            else -> null
        }
    }

    fun validatePhoneNumber(value: String): String? {
        return when {
            value.isBlank() -> resourceProvider.getString(R.string.woo_shipping_field_required_error)
            value.contains(Regex("\\d")).not() -> {
                resourceProvider.getString(R.string.shipping_label_destination_address_phone_invalid)
            }

            else -> null
        }
    }

    fun isPhoneValidForShippingLabel(phone: String): Boolean {
        return phone.isNotBlank() && phone.contains(Regex("\\d"))
    }

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
    }
}
