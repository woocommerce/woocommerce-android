package com.woocommerce.android.ui.orders.wooshippinglabels.packages.datasource

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.CustomPackageCreationRequestData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking.WooShippingLabelPackageRestClient
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooShippingLabelPackageRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val packageMapper: WooShippingLabelPackageMapper,
    private val packageRestClient: WooShippingLabelPackageRestClient
) {
    suspend fun fetchAllStorePackages(
        site: SiteModel = selectedSite.get()
    ) = with(packageRestClient.fetchShippingLabelPackages(site)) {
        result.takeIf { isError.not() }
            ?.let { packageMapper(it) }
            ?.let { WooResult(it) }
            ?: WooResult(error)
    }

    suspend fun createCustomPackage(
        requestData: List<CustomPackageCreationRequestData>,
        site: SiteModel = selectedSite.get(),
    ) = with(packageRestClient.postNewCustomPackage(site, requestData)) {
        result.takeIf { isError.not() }
            ?.let { packageMapper(it) }
            ?.let { WooResult(it) }
            ?: WooResult(error)
    }

    suspend fun saveCarrierPackage(
        savedPackageId: String,
        parentCarrierId: String,
        site: SiteModel = selectedSite.get(),
    ) = with(
        packageRestClient.postPredefinedPackages(
            site, mapOf(parentCarrierId to listOf(savedPackageId))
        )
    ) {
        result.takeIf { isError.not() }
            ?.let { packageMapper(it) }
            ?.let { WooResult(it) }
            ?: WooResult(error)
    }

    suspend fun deleteSavedCarrierPackage(
        packageId: String,
        site: SiteModel = selectedSite.get(),
    ) = with(packageRestClient.deleteSavedCarrierPackage(site, packageId)) {
        result.takeIf { isError.not() }
            ?.let { packageMapper(it) }
            ?.let { WooResult(it) }
            ?: WooResult(error)
    }
}
