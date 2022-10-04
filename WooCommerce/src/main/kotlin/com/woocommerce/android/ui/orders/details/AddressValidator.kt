package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

const val EMPTY_NAME_ERROR = "Customer name and company are empty"
const val EMPTY_ADDRESS_ERROR = "Address is empty"
const val EMPTY_CITY_ERROR = "City is empty"
const val EMPTY_POSTCODE_ERROR = "Postcode is empty"
const val EMPTY_STATE_ERROR = "State is empty"
const val EMPTY_COUNTRY_ERROR = "Country is empty"
const val INVALID_PHONE_ERROR = "The phone number is invalid"

class AddressValidator @Inject constructor(
    private val coroutineDispatchers: CoroutineDispatchers,
    private val shippingLabelAddressValidator: ShippingLabelAddressValidator,
    private val orderDetailRepository: OrderDetailRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    suspend fun validate(orderId: Long, address: Address) {
        val hasValidFields = fieldValidation(orderId, address)

        // Exit the function earlier because the address is not valid
        if (hasValidFields.not()) return

        withContext(coroutineDispatchers.io) {
            orderDetailRepository.getWooServicesPluginInfo().takeIf { plugin ->
                plugin.isInstalled && plugin.isActive
            }?.let {
                val validationResult = shippingLabelAddressValidator.validateAddress(
                    address = address,
                    type = ShippingLabelAddressValidator.AddressType.DESTINATION,
                    requiresPhoneNumber = false
                )

                val message = when (validationResult) {
                    is ShippingLabelAddressValidator.ValidationResult.Invalid -> {
                        validationResult.message
                    }
                    is ShippingLabelAddressValidator.ValidationResult.NotFound -> {
                        validationResult.message
                    }
                    is ShippingLabelAddressValidator.ValidationResult.Error -> {
                        validationResult.type.name
                    }
                    ShippingLabelAddressValidator.ValidationResult.NameMissing -> {
                        EMPTY_NAME_ERROR
                    }
                    ShippingLabelAddressValidator.ValidationResult.PhoneInvalid -> {
                        INVALID_PHONE_ERROR
                    }
                    else -> {
                        // If it is not an error scenario exit the function
                        return@withContext
                    }
                }

                analyticsTrackerWrapper.track(
                    AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
                    mapOf(
                        AnalyticsTracker.KEY_ERROR_MESSAGE to message,
                        AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_REMOTE,
                        AnalyticsTracker.KEY_ORDER_ID to orderId
                    )
                )
            }
        }
    }

    private fun fieldValidation(orderId: Long, address: Address): Boolean {
        val errors = mutableListOf<String>()

        with(address) {
            // The name or the company name should not be empty
            if (firstName.isEmpty() && lastName.isEmpty() && company.isEmpty()) {
                errors.add(EMPTY_NAME_ERROR)
            }
            if (address1.isEmpty()) {
                errors.add(EMPTY_ADDRESS_ERROR)
            }
            if (city.isEmpty()) {
                errors.add(EMPTY_CITY_ERROR)
            }
            if (postcode.isEmpty()) {
                errors.add(EMPTY_POSTCODE_ERROR)
            }
            if (state.isNotEmpty().not()) {
                errors.add(EMPTY_STATE_ERROR)
            }
            if (country == Location.EMPTY) {
                errors.add(EMPTY_COUNTRY_ERROR)
            }
        }

        if (errors.isNotEmpty()) {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.ORDER_ADDRESS_VALIDATION_ERROR,
                mapOf(
                    AnalyticsTracker.KEY_ERROR_MESSAGE to errors.joinToString(", "),
                    AnalyticsTracker.KEY_VALIDATION_SCENARIO to AnalyticsTracker.VALUE_VALIDATION_SCENARIO_LOCAL,
                    AnalyticsTracker.KEY_ORDER_ID to orderId
                )
            )
        }

        return errors.isEmpty()
    }
}
