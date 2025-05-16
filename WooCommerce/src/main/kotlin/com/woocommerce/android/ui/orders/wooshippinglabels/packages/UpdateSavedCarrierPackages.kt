package com.woocommerce.android.ui.orders.wooshippinglabels.packages

import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.WooShippingLabelPackageRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import javax.inject.Inject

class UpdateSavedCarrierPackages @Inject constructor(
    private val repository: WooShippingLabelPackageRepository
) {
    suspend operator fun invoke(
        savePackage: Boolean,
        packageId: String,
        carrierPackages: Map<Carrier, List<CarrierPackageGroup>>
    ) {
        if (savePackage) {
            repository.saveCarrierPackage(
                packageId, findCarrierIdForPackageId(packageId, carrierPackages)
            )
        } else {
            repository.deleteSavedCarrierPackage(packageId = packageId)
        }
    }

    private fun findCarrierIdForPackageId(
        packageId: String,
        carrierPackages: Map<Carrier, List<CarrierPackageGroup>>?
    ): String {
        return carrierPackages
            ?.entries
            ?.firstNotNullOfOrNull { (carrier, packageGroupList) ->
                packageGroupList
                    .flatMap { it.packages }
                    .find { it.id == packageId }
                    ?.let { carrier.id }
            } ?: ""
    }
}
