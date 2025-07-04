package com.woocommerce.android.ui.orders.wooshippinglabels.packages

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.WooShippingLabelPackageRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import javax.inject.Inject

class UpdateSavedCarrierPackages @Inject constructor(
    private val repository: WooShippingLabelPackageRepository,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(
        savePackage: Boolean,
        packageId: String,
        isUserDefined: Boolean,
        carrierPackages: Map<Carrier, List<CarrierPackageGroup>>? = null
    ) {
        if (savePackage) {
            carrierPackages?.let {
                repository.saveCarrierPackage(
                    packageId,
                    findCarrierIdForPackageId(packageId, carrierPackages),
                    selectedSite.get()
                )
            } ?: WooLog.w(T.SHIPPING_LABELS, "Carrier packages should not be null when saving a package")
        } else {
            repository.deleteSavedCarrierPackage(packageId, isUserDefined, selectedSite.get())
        }
    }

    private fun findCarrierIdForPackageId(
        packageId: String,
        carrierPackages: Map<Carrier, List<CarrierPackageGroup>>
    ): String = carrierPackages.entries.firstNotNullOfOrNull { (carrier, packageGroupList) ->
        packageGroupList
            .flatMap { it.packages }
            .find { it.id == packageId }
            ?.let { carrier.id }
    } ?: ""
}
