package com.woocommerce.android.ui.orders.wooshippinglabels.customs.products

import android.os.Parcelable
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsItem
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.EuProductIdentifiers
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.InputValue
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class WooShippingCustomsProductUIModel(
    val productId: Long,
    val name: String,
    val description: InputValue,
    val tariffNumber: InputValue,
    val sku: InputValue = InputValue.Empty,
    val manufacturerPartNumber: InputValue = InputValue.Empty,
    val gtin: InputValue = InputValue.Empty,
    val hasNoBarcode: Boolean = false,
    val valuePerUnit: InputValue,
    val weightPerUnit: InputValue,
    val originCountry: String,
    val originCountryCode: String,
    val quantity: Float,
    val isExpanded: Boolean,
    val formattedPriceAndWeight: String
) : Parcelable {

    fun isValid(includeEuProductIdentifiers: Boolean) = description is InputValue.Data &&
        tariffNumber is InputValue.Data && valuePerUnit is InputValue.Data &&
        weightPerUnit is InputValue.Data && originCountry.isNotBlank() &&
        (
            !includeEuProductIdentifiers ||
                sku is InputValue.Data && manufacturerPartNumber is InputValue.Data && gtin is InputValue.Data
            )

    val euProductIdentifiers: EuProductIdentifiers
        get() = EuProductIdentifiers(
            sku.currentInput,
            manufacturerPartNumber.currentInput,
            gtin.currentInput,
            hasNoBarcode
        )

    val shippingTotalValue: BigDecimal?
        get() = valuePerUnit
            .takeIf { valuePerUnit is InputValue.Data && quantity > 0 }
            ?.run { this as? InputValue.Data }
            ?.currentInput
            ?.toBigDecimalOrNull()
            ?.let { it * quantity.toBigDecimal() }

    val asCustomItem: CustomsItem
        get() = CustomsItem(
            productID = productId,
            description = description.currentInput,
            hsTariffNumber = tariffNumber.currentInput,
            value = valuePerUnit.currentInput.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            weight = weightPerUnit.currentInput.toFloatOrNull() ?: 0F,
            originCountry = originCountry,
            originCountryCode = originCountryCode,
            quantity = quantity,
            euProductIdentifiers = euProductIdentifiers
        )
}
