package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnDisplay
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnPriceType
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnRestrictionsType
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnTitleFormat
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel.AddOnType

@Parcelize
data class ProductAddon(
    val name: String,
    val required: Boolean,
    val description: String,
    val descriptionEnabled: Boolean,
    val max: String,
    val min: String,
    val position: String,
    val adjustPrice: String,
    val restrictions: String,
    val titleFormat: AddOnTitleFormat?,
    val restrictionsType: AddOnRestrictionsType?,
    val priceType: AddOnPriceType?,
    val type: AddOnType?,
    val display: AddOnDisplay?,
    private val price: String,
    private val rawOptions: List<ProductAddonOption>
) : Parcelable {
    /**
     * Some addons comes with a option list containing a empty single [ProductAddonOption]
     * and all the information for that option stored at [ProductAddon] itself.
     *
     * To keep the standard behavior of get price information always through [rawOptions],
     * this property parses this detached [ProductAddon] information to an option list
     */
    val options
        get() = takeIf { isNoPriceAvailableAtOptions }
            ?.let { listOf(rawOptions.single().copy(priceType = priceType, price = price)) }
            ?: rawOptions

    private val isNoPriceAvailableAtOptions
        get() = (rawOptions.size == 1) && rawOptions.single().price.isEmpty()
}

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
        rawOptions = options?.map { it.toAppModel() } ?: emptyList()
    )

fun WCProductAddonModel.ProductAddonOption.toAppModel() =
    ProductAddonOption(
        priceType = priceType,
        label = label ?: "",
        price = price ?: "",
        image = image ?: ""
    )

private fun Int?.toBoolean() = this == 1
