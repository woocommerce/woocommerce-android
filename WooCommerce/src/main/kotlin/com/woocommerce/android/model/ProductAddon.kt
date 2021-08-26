package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.model.ProductAddon.*
import kotlinx.parcelize.Parcelize
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

    enum class Type {
        MultipleChoice,
        Checkbox,
        CustomText,
        CustomTextArea,
        FileUpload,
        CustomPrice,
        InputMultiplier,
        Heading
    }

    enum class Display {
        Select,
        RadioButton,
        Images
    }

    enum class TitleFormat {
        Label,
        Heading,
        Hide
    }

    enum class Restrictions {
        AnyText,
        OnlyLetters,
        OnlyNumbers,
        OnlyLettersNumbers,
        Email
    }

    enum class PriceType {
        FlatFee,
        QuantityBased,
        PercentageBased
    }
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
        titleFormat = TitleFormat.valueOf(addon.titleFormat.toString()),
        restrictionsType = Restrictions.valueOf(addon.restrictions.toString()),
        priceType = PriceType.valueOf(addon.priceType.toString()),
        type = Type.valueOf(addon.type.toString()),
        display = Display.valueOf(addon.display.toString()),
        rawOptions = options.map { it.toAppModel() }
    )

fun AddonOptionEntity.toAppModel() =
    ProductAddonOption(
        priceType = PriceType.valueOf(priceType.toString()),
        label = label ?: "",
        price = price ?: "",
        image = image ?: ""
    )
