package com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PackageType
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.PackageDAO
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class PackageData(
    val id: String,
    val name: String,
    val dimensions: String,
    val weight: String,
    val isSelected: Boolean,
    val isLetter: Boolean,
    val isPredefined: Boolean = false,
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

        fun fromPackageDAO(
            dao: PackageDAO,
            isSelected: Boolean = false,
            isPredefined: Boolean = true
        ): PackageData = PackageData(
            id = dao.id,
            name = dao.name,
            dimensions = dao.dimensions,
            weight = dao.weight,
            isSelected = isSelected,
            isPredefined = isPredefined,
            isLetter = dao.isLetter,
            dimensionUnit = dao.dimensionUnit,
            weightUnit = dao.weightUnit,
            groupName = dao.groupName
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
    val name: String? = null
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

    fun toPackageData(dimensionUnit: String) = PackageData(
        id = "custom_package",
        name = name.orEmpty(),
        dimensions = "$length x $width x $height",
        weight = weight.orEmpty(),
        isSelected = true,
        isLetter = type == PackageType.ENVELOPE,
        dimensionUnit = dimensionUnit,
        isPredefined = saveAsTemplate
    )

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
        id = "usps",
        name = "USPS",
        logoRes = R.drawable.usps_logo
    )

    data object DHL : Carrier(
        id = "dhl",
        name = "DHL",
        logoRes = R.drawable.dhl_logo
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
