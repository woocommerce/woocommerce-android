package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.ContentType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.RestrictionType
import javax.inject.Inject

class WooShippingCustomsValidator @Inject constructor(
    private val validateHSTariffNumber: ValidateHSTariffNumber,
    private val validateITN: ValidateITN
) {
    fun validate(
        customsData: CustomsData,
        originCountryCode: String,
        destinationCountryCode: String
    ): FormValidationResult {
        val itnValidationResult = validateITN.invoke(customsData, destinationCountryCode)
        if (itnValidationResult !is ValidateITN.ITNValidationResult.Valid) {
            return if (itnValidationResult is ValidateITN.ITNValidationResult.Missing) {
                FormValidationResult.ItnMissing
            } else {
                FormValidationResult.Invalid
            }
        }

        val isValid = validateContentType(customsData.contentType, customsData.contentDescription).isValid &&
            validateRestrictionType(customsData.restrictionType, customsData.restrictionDescription).isValid &&
            customsData.items.all {
                validateHSTariffNumber(it.hsTariffNumber, destinationCountryCode).isValid &&
                    validateProductDescription(it.description, originCountryCode, destinationCountryCode).isValid &&
                    validateProductValue(it.value.toString()).isValid &&
                    validateProductWeight(it.weight.toString()).isValid
            }

        return if (isValid) {
            FormValidationResult.Valid
        } else {
            FormValidationResult.Invalid
        }
    }

    fun validateITN(customsData: CustomsData, destinationCountryCode: String): FieldValidationResult {
        fun ValidateITN.ITNMissingCause.errorMessage() = when (this) {
            ValidateITN.ITNMissingCause.TotalValue ->
                UiString.UiStringRes(R.string.woo_shipping_labels_customs_itn_required_total_value)

            is ValidateITN.ITNMissingCause.HSTariffValue ->
                UiString.UiStringRes(
                    stringRes = R.string.woo_shipping_labels_customs_itn_required_hs_tariff_value,
                    params = listOf(UiString.UiStringText(this.hsTariffNumber))
                )

            ValidateITN.ITNMissingCause.DestinationCountry ->
                UiString.UiStringRes(R.string.woo_shipping_labels_customs_itn_required_destination_country)
        }

        return validateITN.invoke(customsData, destinationCountryCode).let {
            when (it) {
                ValidateITN.ITNValidationResult.Valid -> FieldValidationResult.Valid
                is ValidateITN.ITNValidationResult.Missing -> FieldValidationResult.Invalid(
                    errorMessage = it.cause.errorMessage()
                )

                ValidateITN.ITNValidationResult.InvalidFormat -> FieldValidationResult.Invalid(
                    errorMessageId = R.string.woo_shipping_labels_customs_itn_error_message
                )
            }
        }
    }

    fun validateContentType(contentType: ContentType, contentDescription: String): FieldValidationResult {
        return when (contentType) {
            ContentType.OTHER -> when (contentDescription.isBlank()) {
                false -> FieldValidationResult.Valid
                true -> FieldValidationResult.Invalid(
                    errorMessageId = R.string.woo_shipping_labels_customs_other_error_message
                )
            }

            else -> FieldValidationResult.Valid
        }
    }

    fun validateRestrictionType(
        restrictionType: RestrictionType,
        restrictionDescription: String
    ): FieldValidationResult {
        return when (restrictionType) {
            RestrictionType.OTHER -> when (restrictionDescription.isBlank()) {
                false -> FieldValidationResult.Valid
                true -> FieldValidationResult.Invalid(
                    errorMessageId = R.string.woo_shipping_labels_customs_other_error_message
                )
            }

            else -> FieldValidationResult.Valid
        }
    }

    fun validateHSTariffNumber(tariffNumber: String, destinationCountryCode: String): FieldValidationResult {
        return validateHSTariffNumber.invoke(
            tariffNumber = tariffNumber,
            destinationCountryCode = destinationCountryCode
        )
    }

    fun validateProductDescription(
        description: String,
        originCountryCode: String,
        destinationCountryCode: String
    ): FieldValidationResult = when {
        description.isBlank() -> FieldValidationResult.Invalid(
            errorMessageId = R.string.woo_shipping_labels_customs_product_details_description_missing
        )

        isUSPSDomesticMailShipment(originCountryCode, destinationCountryCode) &&
            description.length > MAX_ITEM_DESCRIPTION_LENGTH -> FieldValidationResult.Invalid(
            errorMessage = UiString.UiStringRes(
                stringRes = R.string.woo_shipping_labels_customs_product_details_description_too_long,
                params = listOf(UiString.UiStringText(MAX_ITEM_DESCRIPTION_LENGTH.toString()))
            )
        )

        else -> FieldValidationResult.Valid
    }

    private fun isUSPSDomesticMailShipment(
        originCountryCode: String,
        destinationCountryCode: String
    ): Boolean = originCountryCode in ACCEPTED_USPS_ORIGIN_COUNTRIES &&
        destinationCountryCode in ACCEPTED_USPS_ORIGIN_COUNTRIES

    fun validateProductValue(value: String) = when (value.isBlank()) {
        false -> FieldValidationResult.Valid
        true -> FieldValidationResult.Invalid(
            errorMessageId = R.string.woo_shipping_labels_customs_product_details_value_required
        )
    }

    fun validateProductWeight(weight: String) = when {
        weight.isBlank() -> FieldValidationResult.Invalid(
            errorMessageId = R.string.woo_shipping_labels_customs_product_details_value_required
        )

        weight.toFloatOrNull() == null || weight.toFloat() == 0f -> FieldValidationResult.Invalid(
            errorMessageId = R.string.woo_shipping_labels_customs_product_details_weight_invalid
        )

        else -> FieldValidationResult.Valid
    }

    enum class FormValidationResult {
        Valid, ItnMissing, Invalid
    }

    sealed interface FieldValidationResult {
        val isValid: Boolean
            get() = this is Valid

        data class Invalid(val errorMessage: UiString) : FieldValidationResult {
            constructor(@StringRes errorMessageId: Int) : this(UiString.UiStringRes(errorMessageId))
        }

        data object Valid : FieldValidationResult
    }

    companion object {
        const val MAX_ITEM_DESCRIPTION_LENGTH = 30

        val ACCEPTED_USPS_ORIGIN_COUNTRIES = arrayOf(
            "US", // United States
            "PR", // Puerto Rico
            "VI", // Virgin Islands
            "GU", // Guam
            "AS", // American Samoa
            "UM", // United States Minor Outlying Islands
            "MH", // Marshall Islands
            "FM", // Micronesia
            "MP", // Northern Mariana Islands
        )
    }
}
