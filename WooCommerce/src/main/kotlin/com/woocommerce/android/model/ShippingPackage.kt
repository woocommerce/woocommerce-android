package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.CustomPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption

@Parcelize
data class ShippingPackage(
    val id: String,
    val title: String,
    val isLetter: Boolean,
    val category: String,
    val dimensions: PackageDimensions
) : Parcelable {
    companion object {
        const val CUSTOM_PACKAGE_CATEGORY = "custom"
    }
}

@Parcelize
data class PackageDimensions(
    val length: Float,
    val width: Float,
    val height: Float
) : Parcelable

fun CustomPackage.toAppModel(): ShippingPackage {
    val dimensionsParts = dimensions.split("x")
    return ShippingPackage(
        id = title,
        title = title,
        isLetter = isLetter,
        dimensions = PackageDimensions(
            length = dimensionsParts[0].trim().toFloat(),
            width = dimensionsParts[1].trim().toFloat(),
            height = dimensionsParts[2].trim().toFloat()
        ),
        category = ShippingPackage.CUSTOM_PACKAGE_CATEGORY
    )
}

fun PredefinedOption.toAppModel(): List<ShippingPackage> {
    return predefinedPackages.map {
        val dimensionsParts = it.dimensions.split("x")
        ShippingPackage(
            id = it.id,
            title = it.title,
            isLetter = it.isLetter,
            dimensions = PackageDimensions(
                length = dimensionsParts[0].trim().toFloat(),
                width = dimensionsParts[1].trim().toFloat(),
                height = dimensionsParts[2].trim().toFloat()
            ),
            category = this.title
        )
    }
}
