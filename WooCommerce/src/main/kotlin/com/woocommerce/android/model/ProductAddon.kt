package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel

@Parcelize
data class ProductAddon(
    val name: String,
    val description: String,
    val descriptionEnabled: String,
    val max: String,
    val min: String,
    val position: String,
    val price: String
) : Parcelable

fun WCProductAddonModel.toAppModel() =
    ProductAddon(
        name = this.name ?: "",
        description = this.description ?: "",
        descriptionEnabled = this.descriptionEnabled ?: "",
        max = this.max ?: "",
        min = this.min ?: "",
        position = this.position ?: "",
        price = this.price ?: ""
    )
