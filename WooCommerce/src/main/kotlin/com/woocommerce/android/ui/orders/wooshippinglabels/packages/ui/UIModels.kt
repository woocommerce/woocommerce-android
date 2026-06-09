package com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PackageType
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.CarrierType
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.Package

@Parcelize
data class PackageData(
    val id: String,
    val name: String,
    val dimensions: String,
    val weight: String,
    val isSelected: Boolean,
    val isLetter: Boolean,
    val isStarred: Boolean = false,
    val isUserDefined: Boolean = false,
    val dimensionUnit: String = "cm",
    val weightUnit: String = "kg",
    val groupName: String? = null
) : Parcelable {
    @IgnoredOnParcel
    val length: String

    @IgnoredOnParcel
    val width: String

    @IgnoredOnParcel
    val height: String

    init {
        val dimensionList = dimensions.split("x")
        length = dimensionList.getOrNull(0).orEmpty().trim()
        width = dimensionList.getOrNull(1).orEmpty().trim()
        height = dimensionList.getOrNull(2).orEmpty().trim()
    }

    val safeHeight: Double
        get() {
            val parsed = height.toDoubleOrNull() ?: return DEFAULT_HEIGHT
            return if (parsed > 0) parsed else MIN_HEIGHT
        }

    val descriptionResId: Int
        get() = when (isLetter) {
            true -> R.string.woo_shipping_labels_package_creation_envelope_type
            false -> R.string.woo_shipping_labels_package_creation_box_type
        }

    val dimensionForDisplay
        get() = "$dimensions $dimensionUnit"

    val weightForDisplay
        get() = "$weight $weightUnit"

    companion object {
        val EMPTY = PackageData(
            id = "",
            name = "",
            dimensions = "",
            weight = "",
            isSelected = false,
            isLetter = false,
            groupName = null
        )

        /**
         * Default height when missing from the package dimensions.
         * TODO confirm this value when WOOSHIP-1449 is done.
         */
        const val DEFAULT_HEIGHT = 5.0
        const val MIN_HEIGHT = 0.25

        fun fromPackageEntity(entity: Package, isSelected: Boolean = false) = PackageData(
            id = entity.id ?: EMPTY.id,
            name = entity.name ?: EMPTY.name,
            dimensions = entity.dimensions ?: EMPTY.dimensions,
            weight = entity.weight ?: EMPTY.weight,
            isSelected = isSelected,
            isUserDefined = entity.isUserDefined,
            isLetter = entity.isLetter,
            dimensionUnit = entity.dimensionUnit ?: EMPTY.dimensionUnit,
            weightUnit = entity.weightUnit ?: EMPTY.weightUnit,
            groupName = entity.groupName,
            isStarred = entity.saved
        )
    }
}

@Parcelize
data class CustomPackageCreationData(
    val type: PackageType,
    val length: String,
    val width: String,
    val height: String,
    val saveAsTemplate: Boolean,
    val weight: String? = null,
    val name: String? = null,
    val invalidDimensionError: Boolean = false
) : Parcelable {
    val isValid: Boolean
        get() = height.isNotEmpty() && length.isNotEmpty() && width.isNotEmpty() && isTemplateConfigured

    val dimensions: String
        get() = "$length x $width x $height"

    private val isTemplateConfigured: Boolean
        get() {
            if (saveAsTemplate.not()) return true

            return name.isNotNullOrEmpty() && weight.isNotNullOrEmpty()
        }

    val invalidDimensions: Boolean
        get() = (length.toDoubleOrNull() ?: 0.0) <= 0.0 ||
            (width.toDoubleOrNull() ?: 0.0) <= 0.0 ||
            (height.toDoubleOrNull() ?: 0.0) <= 0.0

    fun toPackageData(dimensionUnit: String) = PackageData(
        id = "custom_package",
        name = name.orEmpty(),
        dimensions = "$length x $width x $height",
        weight = weight.orEmpty(),
        isSelected = true,
        isLetter = type == PackageType.ENVELOPE,
        dimensionUnit = dimensionUnit,
        isUserDefined = saveAsTemplate
    )

    fun copyWithUpdatedError() = copy(invalidDimensionError = invalidDimensions)

    companion object {
        val EMPTY = CustomPackageCreationData(
            type = PackageType.BOX,
            length = "",
            width = "",
            height = "",
            weight = "",
            saveAsTemplate = false
        )
    }
}

@Parcelize
data class CarrierPackageGroup(
    val groupName: String,
    val packages: List<PackageData>
) : Parcelable

@Parcelize
sealed class Carrier(
    val id: String,
    val name: String,
    val logoRes: Int? = null,
) : Parcelable {
    data object USPS : Carrier(
        id = CarrierType.USPS.id,
        name = "USPS",
        logoRes = R.drawable.usps_logo
    )

    data object FEDEX : Carrier(
        id = CarrierType.FEDEX.id,
        name = "FedEx",
        logoRes = R.drawable.fedex_logo
    )

    data object DHL : Carrier(
        id = CarrierType.DHL.id,
        name = "DHL",
        logoRes = R.drawable.dhl_logo
    )

    data object UPS : Carrier(
        id = CarrierType.UPS.id,
        name = "UPS",
        logoRes = R.drawable.ups_logo
    )
}

@Parcelize
data class StorePredefinedPackages(
    val carrierPackages: Map<Carrier, List<CarrierPackageGroup>>,
    val savedPackages: List<PackageData>
) : Parcelable

@Parcelize
data class StoreOptionsForPackages(
    val currencySymbol: String,
    val dimensionUnit: String,
    val weightUnit: String,
    val originCountry: String
) : Parcelable {
    companion object {
        val DEFAULT = StoreOptionsForPackages(
            currencySymbol = "USD",
            dimensionUnit = "cm",
            weightUnit = "kg",
            originCountry = "US"
        )
    }
}
