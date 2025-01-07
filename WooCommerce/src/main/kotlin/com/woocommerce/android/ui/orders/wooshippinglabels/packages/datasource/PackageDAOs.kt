package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

data class StorePackagesDAO(
    val storeOptions: StoreOptionsDAO,
    val savedPackages: List<PackageDAO>,
    val carrierPackages: Map<CarrierType, CarrierDAO>
)

data class PackageDAO(
    val id: String,
    val name: String,
    val dimensions: String,
    val weight: String,
    val isLetter: Boolean,
    val dimensionUnit: String,
    val weightUnit: String,
    val groupName: String? = null
)

data class CarrierDAO(
    val packageGroup: List<CarrierPackageGroupDAO>
)

data class CarrierPackageGroupDAO(
    val description: String,
    val packages: List<PackageDAO>
)

data class StoreOptionsDAO(
    val currencySymbol: String,
    val dimensionUnit: String,
    val weightUnit: String,
    val originCountry: String
)

enum class CarrierType {
    USPS,
    DHL
}
