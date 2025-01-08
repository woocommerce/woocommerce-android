package com.woocommerce.android.ui.orders.wooshippinglabels.packages.networking

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class WooShippingLabelPackageRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {
    suspend fun fetchShippingLabelPackages(
        site: SiteModel
    ): WooPayload<PackageResponse> {
        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = URL,
            clazz = PackageResponse::class.java,
        ).toWooPayload()
    }

    suspend fun postNewCustomPackage(
        site: SiteModel,
        requestData: List<CustomPackageCreationRequestData>
    ): WooPayload<CustomPackageCreationResponse> {
        return wooNetwork.executePostGsonRequest(
            site = site,
            path = URL,
            body = mapOf("custom" to requestData),
            clazz = CustomPackageCreationResponse::class.java,
        ).toWooPayload()
    }

    companion object {
        private const val URL = "/wcshipping/v1/packages"
    }
}
