package com.woocommerce.android.model

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.ShippingPackage.Companion.INDIVIDUAL_PACKAGE
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.CustomPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption.PredefinedPackage

@Parcelize
data class ShippingPackage(
    val id: String,
    val title: String,
    val isLetter: Boolean,
    val category: String,
    val dimensions: PackageDimensions,
    val boxWeight: Float,
    val carrierId: String = "" /* Can be empty, only needed by predefined packages */
) : Parcelable {
    companion object {
        const val CUSTOM_PACKAGE_CATEGORY = "custom"
        const val INDIVIDUAL_PACKAGE = "individual"
    }

    @IgnoredOnParcel
    val isIndividual
        get() = id == INDIVIDUAL_PACKAGE

    fun toCustomPackageDataModel(): CustomPackage {
        return CustomPackage(
            title = title,
            isLetter = isLetter,
            dimensions = dimensions.toString(),
            boxWeight = boxWeight
        )
    }

    fun toPredefinedOptionDataModel(): PredefinedOption {
        return PredefinedOption(
            title = category,
            carrier = carrierId,
            predefinedPackages = listOf(
                PredefinedPackage(
                    id = id,
                    title = title,
                    isLetter = isLetter,
                    dimensions = dimensions.toString(),
                    boxWeight = boxWeight
                )
            )
        )
    }
}

@Parcelize
data class PackageDimensions(
    val length: Float,
    val width: Float,
    val height: Float
) : Parcelable {
    @IgnoredOnParcel
    val isValid
        get() = length > 0f && width > 0f && height > 0f

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
            category = this.title,
            carrierId = this.carrier
        )
    }
}

enum class CustomPackageType(@StringRes val stringRes: Int) {
    BOX(R.string.shipping_label_create_custom_package_field_type_box),
    ENVELOPE(R.string.shipping_label_create_custom_package_field_type_envelope)
}

fun ShippingLabelPackage.Item.createIndividualShippingPackage(product: IProduct?): ShippingPackage {
    return ShippingPackage(
        id = INDIVIDUAL_PACKAGE,
        title = name,
        isLetter = false,
        dimensions = PackageDimensions(
            length = product?.length ?: 0f,
            width = product?.width ?: 0f,
            height = product?.height ?: 0f
        ),
        boxWeight = 0f,
        category = INDIVIDUAL_PACKAGE
    )
}
