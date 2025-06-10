package com.woocommerce.android.ui.orders.wooshippinglabels.packages

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.WooShippingLabelPackageRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import javax.inject.Inject

class UpdateSavedCarrierPackages @Inject constructor(
    private val repository: WooShippingLabelPackageRepository,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(
        savePackage: Boolean,
        packageId: String,
        carrierPackages: Map<Carrier, List<CarrierPackageGroup>>
    ) {
        if (savePackage) {
            repository.saveCarrierPackage(
                packageId,
                findCarrierIdForPackageId(packageId, carrierPackages),
                selectedSite.get()
            )
        } else {
            repository.deleteSavedCarrierPackage(
                packageId,
                selectedSite.get()
            )
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
