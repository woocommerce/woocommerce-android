package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.Type
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.Restrictions
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.TitleFormat
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.PriceType
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.Display
import org.wordpress.android.fluxc.persistence.entity.AddonOptionEntity
import org.wordpress.android.fluxc.persistence.entity.AddonWithOptions

@Parcelize
data class ProductAddon(
    val name: String,
    val required: Boolean,
    val description: String,
    val descriptionEnabled: Boolean,
    val max: Long,
    val min: Long,
    val position: Int,
    val adjustPrice: Boolean,
    val titleFormat: TitleFormat?,
    val restrictionsType: Restrictions?,
    val priceType: PriceType?,
    val type: Type?,
    val display: Display?,
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
    val priceType: PriceType?,
    val label: String,
    val price: String,
    val image: String
) : Parcelable

fun AddonWithOptions.toAppModel() =
    ProductAddon(
        name = addon.name,
        description = addon.description,
        descriptionEnabled = addon.descriptionEnabled,
        max = addon.max ?: 0,
        min = addon.min ?: 0,
        position = addon.position,
        price = addon.price ?: "",
        adjustPrice = addon.priceAdjusted ?: false,
        required = addon.required,
        titleFormat = addon.titleFormat,
        restrictionsType = addon.restrictions,
        priceType = addon.priceType,
        type = addon.type,
        display = addon.display,
        rawOptions = options.map { it.toAppModel() }
    )

fun AddonOptionEntity.toAppModel() =
    ProductAddonOption(
        priceType = priceType,
        label = label ?: "",
        price = price ?: "",
        image = image ?: ""
    )
