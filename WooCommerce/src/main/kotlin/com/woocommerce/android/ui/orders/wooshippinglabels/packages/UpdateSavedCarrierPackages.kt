package com.woocommerce.android.ui.orders.wooshippinglabels.packages

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource.WooShippingLabelPackageRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
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
    ): Result<Unit> = if (savePackage) {
        val carrierId = carrierPackages?.let { findCarrierIdForPackageId(packageId, it) }
            ?: return Result.failure(IllegalArgumentException("Carrier ID not found for package $packageId"))

        repository.saveCarrierPackage(packageId, carrierId, selectedSite.get()).toResultUnit()
    } else {
        repository.deleteSavedCarrierPackage(packageId, isUserDefined, selectedSite.get()).toResultUnit()
    }

    private fun <T> WooResult<T>.toResultUnit(): Result<Unit> =
        if (isError) Result.failure(WooException(error)) else Result.success(Unit)

    private fun findCarrierIdForPackageId(
        packageId: String,
        carrierPackages: Map<Carrier, List<CarrierPackageGroup>>
    ): String? = carrierPackages.entries.firstNotNullOfOrNull { (carrier, packageGroupList) ->
        packageGroupList
            .flatMap { it.packages }
            .find { it.id == packageId }
            ?.let { carrier.id }
    }
}
