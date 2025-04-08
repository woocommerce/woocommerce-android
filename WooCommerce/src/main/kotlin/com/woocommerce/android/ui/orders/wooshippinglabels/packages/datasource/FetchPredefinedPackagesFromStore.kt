package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PredefinedPackagesState
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.StoreOptionsForPackages
import javax.inject.Inject

class FetchPredefinedPackagesFromStore @Inject constructor(
    private val selectedSite: SelectedSite,
    private val packageRepository: WooShippingLabelPackageRepository
) {
    suspend operator fun invoke(): PredefinedPackagesState {
        val storePackages = selectedSite.getOrNull()
            ?.let { packageRepository.fetchAllStorePackages(it) }
            ?.takeIf { it.isError.not() }
            ?.model
            ?: return PredefinedPackagesState.Error

        return PredefinedPackagesState.Data(
            storeOptions = storePackages.storeOptions.toStoreOptionsForPackages(),
            savedPackages = storePackages.savedPackages
                .map { PackageData.fromPackageDAO(it) },
            carrierPackages = storePackages.filterCarrierData()
        )
    }

    private fun StorePackagesDAO.filterCarrierData(): Map<Carrier, List<CarrierPackageGroup>> {
        val result = mutableMapOf<Carrier, List<CarrierPackageGroup>>()
        carrierPackages.parseCarrierData(CarrierType.USPS).takeIf { it.isNotEmpty() }?.let { carrierData ->
            result[Carrier.USPS] = carrierData
        }

        carrierPackages.parseCarrierData(CarrierType.DHL).takeIf { it.isNotEmpty() }?.let { carrierData ->
            result[Carrier.DHL] = carrierData
        }

        return result
    }

    private fun StoreOptionsDAO.toStoreOptionsForPackages() =
        StoreOptionsForPackages(
            currencySymbol = currencySymbol,
            dimensionUnit = dimensionUnit,
            weightUnit = weightUnit,
            originCountry = originCountry
        )

    private fun Map<CarrierType, CarrierDAO>.parseCarrierData(
        carrierType: CarrierType
    ) = get(carrierType)?.let {
        it.packageGroup.map { group ->
            CarrierPackageGroup(
                groupName = group.description,
                packages = group.packages.map { packageItem ->
                    PackageData.fromPackageDAO(packageItem)
                }
            )
        }
    } ?: emptyList()
}
