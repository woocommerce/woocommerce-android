package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.ContentType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.RestrictionType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel
import javax.inject.Inject

class WooShippingCustomsValidator @Inject constructor(
    private val validateHSTariffNumber: ValidateHSTariffNumber,
    private val validateITN: ValidateITN
) {
    fun validate(customsData: CustomsData, destinationCountryCode: String): ValidationResult {
        val isValid = validateITN(customsData, destinationCountryCode).isValid &&
            validateContentType(customsData.contentType, customsData.contentDescription).isValid &&
            validateRestrictionType(customsData.restrictionType, customsData.restrictionDescription).isValid &&
            customsData.items.all {
                validateHSTariffNumber(it.hsTariffNumber, destinationCountryCode).isValid &&
                    validateProductDescription(it.description).isValid &&
                    validateProductValue(it.value.toString()).isValid &&
                    validateProductWeight(it.weight.toString()).isValid
            }

        return if (isValid) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                errorMessage = UiString.UiStringText("") // This is not used
            )
        }
    }

    fun validateITN(customsData: CustomsData, destinationCountryCode: String): ValidationResult {
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
                ValidateITN.ITNValidationResult.Valid -> ValidationResult.Valid
                is ValidateITN.ITNValidationResult.Missing -> ValidationResult.Invalid(
                    errorMessage = it.cause.errorMessage()
                )

                ValidateITN.ITNValidationResult.InvalidFormat -> ValidationResult.Invalid(
                    errorMessageId = R.string.woo_shipping_labels_customs_itn_error_message
                )
            }
        }
    }

    fun validateContentType(contentType: ContentType, contentDescription: String): ValidationResult {
        return when (contentType) {
            ContentType.OTHER -> when (contentDescription.isBlank()) {
                false -> ValidationResult.Valid
                true -> ValidationResult.Invalid(
                    errorMessageId = R.string.woo_shipping_labels_customs_other_error_message
                )
            }

            else -> ValidationResult.Valid
        }
    }

    fun validateRestrictionType(
        restrictionType: RestrictionType,
        restrictionDescription: String
    ): ValidationResult {
        return when (restrictionType) {
            RestrictionType.OTHER -> when (restrictionDescription.isBlank()) {
                false -> ValidationResult.Valid
                true -> ValidationResult.Invalid(
                    errorMessageId = R.string.woo_shipping_labels_customs_other_error_message
                )
            }

            else -> ValidationResult.Valid
        }
    }

    fun validateHSTariffNumber(tariffNumber: String, destinationCountryCode: String): ValidationResult {
        return validateHSTariffNumber.invoke(
            tariffNumber = tariffNumber,
            destinationCountryCode = destinationCountryCode
        ).let { inputValue ->
            when (inputValue) {
                is WooShippingCustomsFormViewModel.InputValue.Error -> ValidationResult.Invalid(
                    errorMessage = inputValue.errorMessageId
                )

                else -> ValidationResult.Valid
            }
        }
    }

    fun validateProductDescription(description: String) = when (description.isBlank()) {
        false -> ValidationResult.Valid
        true -> ValidationResult.Invalid(
            errorMessageId = R.string.woo_shipping_labels_customs_product_details_description_missing
        )
    }

    fun validateProductValue(value: String) = when (value.isBlank()) {
        false -> ValidationResult.Valid
        true -> ValidationResult.Invalid(
            errorMessageId = R.string.woo_shipping_labels_customs_product_details_value_required
        )
    }

    fun validateProductWeight(weight: String) = when {
        weight.isBlank() -> ValidationResult.Invalid(
            errorMessageId = R.string.woo_shipping_labels_customs_product_details_value_required
        )

        weight.toFloatOrNull() == null || weight.toFloat() == 0f -> ValidationResult.Invalid(
            errorMessageId = R.string.woo_shipping_labels_customs_product_details_weight_invalid
        )

        else -> ValidationResult.Valid
    }

    sealed interface ValidationResult {
        val isValid: Boolean
            get() = this is Valid

        data class Invalid(val errorMessage: UiString) : ValidationResult {
            constructor(@StringRes errorMessageId: Int) : this(UiString.UiStringRes(errorMessageId))
        }

        data object Valid : ValidationResult
    }
}
