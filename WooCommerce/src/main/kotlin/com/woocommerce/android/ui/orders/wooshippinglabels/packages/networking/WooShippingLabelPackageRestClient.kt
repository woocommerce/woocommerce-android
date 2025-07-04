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
            params = mapOf("features_supported_by_client[]" to UPSDAP_FEATURE),
            clazz = PackageResponse::class.java,
        ).toWooPayload()
    }

    /**
     * Creates a new custom package.
     */
    suspend fun postNewCustomPackage(
        site: SiteModel,
        requestData: List<CustomPackageCreationRequestData>
    ): WooPayload<PackageCreationResponse> {
        return wooNetwork.executePostGsonRequest(
            site = site,
            path = URL,
            body = mapOf(TYPE_CUSTOM to requestData),
            clazz = PackageCreationResponse::class.java,
        ).toWooPayload()
    }

    /**
     * Updates the saved carrier packages.
     */
    suspend fun postPredefinedPackages(
        site: SiteModel,
        requestData: Map<String, List<String>>
    ): WooPayload<PackageCreationResponse> {
        return wooNetwork.executePostGsonRequest(
            site = site,
            path = URL,
            body = mapOf(TYPE_PREDEFINED to requestData),
            clazz = PackageCreationResponse::class.java,
        ).toWooPayload()
    }

    suspend fun deleteSavedCarrierPackage(
        site: SiteModel,
        packageId: String
    ): WooPayload<PackageCreationResponse> {
        return wooNetwork.executeDeleteGsonRequest(
            site = site,
            path = "$URL/predefined/$packageId",
            clazz = PackageCreationResponse::class.java,
        ).toWooPayload()
    }

    companion object {
        private const val URL = "/wcshipping/v1/packages"
        private const val UPSDAP_FEATURE = "upsdap"
        private const val TYPE_CUSTOM = "custom"
        private const val TYPE_PREDEFINED = "predefined"
    }
}
