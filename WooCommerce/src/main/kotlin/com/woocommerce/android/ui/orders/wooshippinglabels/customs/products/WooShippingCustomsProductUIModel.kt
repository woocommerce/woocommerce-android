package com.woocommerce.android.ui.orders.wooshippinglabels.customs.products

import android.os.Parcelable
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsItem
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.InputValue
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class WooShippingCustomsProductUIModel(
    val productId: Long,
    val name: String,
    val description: InputValue,
    val tariffNumber: InputValue,
    val valuePerUnit: InputValue,
    val weightPerUnit: InputValue,
    val originCountry: String,
    val originCountryCode: String,
    val quantity: Float,
    val isExpanded: Boolean
) : Parcelable {
    val valueAndWeightForDisplay: String
        get() = "${valuePerUnit.currentInput} • ${weightPerUnit.currentInput}"

    val isValid: Boolean
        get() = description is InputValue.Data &&
            tariffNumber is InputValue.Data &&
            valuePerUnit is InputValue.Data &&
            weightPerUnit is InputValue.Data &&
            originCountry.isNotBlank()

    val shippingTotalValue: Float?
        get() = valuePerUnit
            .takeIf { valuePerUnit is InputValue.Data && quantity > 0 }
            ?.run { this as? InputValue.Data }
            ?.currentInput
            ?.toFloatOrNull()
            ?.let { it * quantity }

    val asCustomItem: CustomsItem
        get() = CustomsItem(
            productID = productId,
            description = description.currentInput,
            hsTariffNumber = tariffNumber.currentInput,
            value = valuePerUnit.currentInput.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            weight = weightPerUnit.currentInput.toFloatOrNull() ?: 0F,
            originCountry = originCountry,
            originCountryCode = originCountryCode,
            quantity = quantity
        )
}
