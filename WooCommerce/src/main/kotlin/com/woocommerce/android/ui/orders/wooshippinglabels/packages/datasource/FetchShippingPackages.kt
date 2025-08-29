package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PackagesState
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.StoreOptionsForPackages
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.CarrierPackageGroups
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.StoreOptions
import javax.inject.Inject

class FetchShippingPackages @Inject constructor(
    private val selectedSite: SelectedSite,
    private val packageRepository: WooShippingLabelPackageRepository
) {
    suspend operator fun invoke(): PackagesState {
        val result = packageRepository.fetchShippingPackages(selectedSite.get())
        val model = result.model
        return if (!result.isError && model != null) {
            val storePackages = model
            PackagesState.Data(
                storeOptions = storePackages.storeOptions.toStoreOptionsForPackages(),
                savedPackages = storePackages.savedPackages.map { PackageData.fromPackageEntity(it) },
                carrierPackages = storePackages.filterCarrierData()
            )
        } else {
            PackagesState.Error(result.error.type.name)
        }
    }

    private fun StoreOptions.toStoreOptionsForPackages() = StoreOptionsForPackages(
        currencySymbol = currencySymbol ?: StoreOptionsForPackages.DEFAULT.currencySymbol,
        dimensionUnit = dimensionUnit ?: StoreOptionsForPackages.DEFAULT.dimensionUnit,
        weightUnit = weightUnit ?: StoreOptionsForPackages.DEFAULT.weightUnit,
        originCountry = originCountry ?: StoreOptionsForPackages.DEFAULT.originCountry
    )

    private fun WooShippingPackagesEntity.filterCarrierData(): Map<Carrier, List<CarrierPackageGroup>> = buildMap {
        carrierPackageGroups.parseCarrierData(WooShippingPackagesEntity.CarrierType.USPS)
            .takeIf { it.isNotEmpty() }
            ?.let { packageGroups -> put(Carrier.USPS, packageGroups) }

        carrierPackageGroups.parseCarrierData(WooShippingPackagesEntity.CarrierType.DHL)
            .takeIf { it.isNotEmpty() }
            ?.let { packageGroups -> put(Carrier.DHL, packageGroups) }

        carrierPackageGroups.parseCarrierData(WooShippingPackagesEntity.CarrierType.UPS)
            .takeIf { it.isNotEmpty() }
            ?.let { packageGroups -> put(Carrier.UPS, packageGroups) }
    }

    private fun List<CarrierPackageGroups>.parseCarrierData(
        carrierType: WooShippingPackagesEntity.CarrierType
    ) = find { it.carrierType == carrierType }?.let {
        it.packageGroups?.mapNotNull { group ->
            group.description?.let { description ->
                CarrierPackageGroup(
                    groupName = description,
                    packages = group.packages?.map { packageItem ->
                        PackageData.fromPackageEntity(packageItem)
                    }.orEmpty()
                )
            }
        }
    } ?: emptyList()
}
