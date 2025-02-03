package com.woocommerce.android.ui.orders.wooshippinglabels.address

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class AddressValidationHelper @Inject constructor(
    private val resourceProvider: ResourceProvider
) {
    fun validateAtLeastOneOf(vararg values: String): String? {
        return if (values.all { it.isEmpty() || it.isBlank() }) {
            resourceProvider.getString(R.string.woo_shipping_field_required_error)
        } else {
            null
        }
    }

    fun validateFieldRequired(value: String): String? {
        return if (value.isEmpty() || value.isBlank()) {
            resourceProvider.getString(R.string.woo_shipping_field_required_error)
        } else {
            null
        }
    }

    fun validateUSCustomsPhone(value: String): String? {
        return when {
            value.isEmpty() || value.isBlank() -> resourceProvider.getString(R.string.woo_shipping_field_required_error)
            value.replace(Regex("^1|[^\\d]"), "").length != US_PHONE_NUMBER_LENGTH -> {
                resourceProvider.getString(R.string.shipping_label_destination_address_phone_invalid)
            }

            else -> null
        }
    }

    fun validateCustomsPhone(value: String): String? {
        return when {
            value.isEmpty() || value.isBlank() -> resourceProvider.getString(R.string.woo_shipping_field_required_error)
            value.contains(Regex("\\d")).not() -> {
                resourceProvider.getString(R.string.shipping_label_destination_address_phone_invalid)
            }
            else -> null
        }
    }

    companion object {
        private const val US_PHONE_NUMBER_LENGTH = 10
    }
}
