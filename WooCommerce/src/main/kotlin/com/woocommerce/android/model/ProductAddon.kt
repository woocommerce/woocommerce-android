package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnPriceType
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnType
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnDisplay
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnRestrictionsType
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnTitleFormat

@Parcelize
data class ProductAddon(
    val name: String,
    val required: Boolean,
    val description: String,
    val descriptionEnabled: Boolean,
    val max: String,
    val min: String,
    val position: String,
    val price: String,
    val adjustPrice: String,
    val restrictions: String,
    val titleFormat: AddOnTitleFormat?,
    val restrictionsType: AddOnRestrictionsType?,
    val priceType: AddOnPriceType?,
    val type: AddOnType?,
    val display: AddOnDisplay?,
    val options: List<ProductAddonOption>
) : Parcelable

@Parcelize
data class ProductAddonOption(
    val priceType: AddOnPriceType?,
    val label: String,
    val price: String,
    val image: String
) : Parcelable

fun WCProductAddonModel.toAppModel() =
    ProductAddon(
        name = name ?: "",
        description = description ?: "",
        descriptionEnabled = descriptionEnabled?.toIntOrNull().toBoolean(),
        max = max ?: "",
        min = min ?: "",
        position = position ?: "",
        price = price ?: "",
        adjustPrice = adjustPrice ?: "",
        required = required?.toIntOrNull().toBoolean(),
        restrictions = restrictions ?: "",
        titleFormat = titleFormat,
        restrictionsType = restrictionsType,
        priceType = priceType,
        type = type,
        display = display,
        options = options?.map { it.toAppModel() } ?: emptyList()
    )

fun WCProductAddonModel.ProductAddonOption.toAppModel() =
    ProductAddonOption(
        priceType = priceType,
        label = label ?: "",
        price = price ?: "",
        image = image ?: ""
    )

private fun Int?.toBoolean() = this == 1
