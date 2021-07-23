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
    val description: String,
    val descriptionEnabled: String,
    val max: String,
    val min: String,
    val position: String,
    val price: String,
    val adjustPrice: String,
    val required: String,
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
        name = this.name ?: "",
        description = this.description ?: "",
        descriptionEnabled = this.descriptionEnabled ?: "",
        max = this.max ?: "",
        min = this.min ?: "",
        position = this.position ?: "",
        price = this.price ?: "",
        adjustPrice = this.adjustPrice ?: "",
        required = this.required ?: "",
        restrictions = this.restrictions ?: "",
        titleFormat = this.titleFormat,
        restrictionsType = this.restrictionsType,
        priceType = this.priceType,
        type = this.type,
        display = this.display,
        options = this.options?.map { it.toAppModel() } ?: emptyList()
    )

fun WCProductAddonModel.ProductAddonOption.toAppModel() =
    ProductAddonOption(
        priceType = this.priceType,
        label = this.label ?: "",
        price = this.price ?: "",
        image = this.image ?: ""
    )
