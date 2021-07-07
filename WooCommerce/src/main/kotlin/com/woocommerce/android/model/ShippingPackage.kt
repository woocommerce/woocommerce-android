package com.woocommerce.android.model

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.ShippingPackage.Companion.INDIVIDUAL_PACKAGE
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.CustomPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption

@Parcelize
data class ShippingPackage(
    val id: String,
    val title: String,
    val isLetter: Boolean,
    val category: String,
    val dimensions: PackageDimensions,
    val boxWeight: Float
) : Parcelable {
    companion object {
        const val CUSTOM_PACKAGE_CATEGORY = "custom"
        const val INDIVIDUAL_PACKAGE = "individual"
    }

    fun toCustomPackageDataModel(): CustomPackage {
        return CustomPackage(
            title = title,
            isLetter = isLetter,
            dimensions = dimensions.toString(),
            boxWeight = boxWeight
        )
    }
}

@Parcelize
data class PackageDimensions(
    val length: Float,
    val width: Float,
    val height: Float
) : Parcelable {
    override fun toString(): String {
        return "$length x $width x $height" // This formatting mirrors how it's done in core.
    }
}

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
        boxWeight = boxWeight,
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
            boxWeight = it.boxWeight,
            category = this.title
        )
    }
}

enum class CustomPackageType(@StringRes val stringRes: Int) {
    BOX(R.string.shipping_label_create_custom_package_field_type_box),
    ENVELOPE(R.string.shipping_label_create_custom_package_field_type_envelope)
}

fun Product?.createIndividualShippingPackage(): ShippingPackage {
    return ShippingPackage(
        id = INDIVIDUAL_PACKAGE,
        title = INDIVIDUAL_PACKAGE,
        isLetter = false,
        dimensions = PackageDimensions(
            length = this?.length ?: 0f,
            width = this?.width ?: 0f,
            height = this?.height ?: 0f
        ),
        boxWeight = 0f,
        category = INDIVIDUAL_PACKAGE
    )
}
