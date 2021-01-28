package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.CustomPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption

@Parcelize
data class ShippingPackage(
    val id: String? = null,
    val title: String,
    val isLetter: Boolean,
    val isCustom: Boolean,
    val dimensions: PackageDimensions
) : Parcelable

@Parcelize
data class PackageDimensions(
    val length: Double,
    val width: Double,
    val height: Double
) : Parcelable

fun CustomPackage.toAppModel(): ShippingPackage {
    val dimensionsParts = dimensions.split("x")
    return ShippingPackage(
        title = title,
        isLetter = isLetter,
        dimensions = PackageDimensions(
            length = dimensionsParts[0].trim().toDouble(),
            width = dimensionsParts[0].trim().toDouble(),
            height = dimensionsParts[0].trim().toDouble()
        ),
        isCustom = true
    )
}

fun PredefinedOption.toAppModel(): List<ShippingPackage> {
    return predefinedPackages.map {
        val dimensionsParts = it.dimensions.split("x")
        ShippingPackage(
            title = "$title - ${it.title}",
            isLetter = it.isLetter,
            dimensions = PackageDimensions(
                length = dimensionsParts[0].trim().toDouble(),
                width = dimensionsParts[0].trim().toDouble(),
                height = dimensionsParts[0].trim().toDouble()
            ),
            isCustom = false
        )
    }
}
