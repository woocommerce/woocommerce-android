package com.woocommerce.android.network.shippingmethods

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class ShippingMethodsRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchShippingMethods(site: SiteModel): WooPayload<List<ShippingMethodDto>> {
        val url = WOOCOMMERCE.shipping_methods.pathV3

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<ShippingMethodDto>::class.java,
        ).toWooPayload { methods -> methods.toList() }
    }

    suspend fun fetchShippingMethodsById(site: SiteModel, methodId: String): WooPayload<ShippingMethodDto> {
        val url = WOOCOMMERCE.shipping_methods.id(methodId).pathV3

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = ShippingMethodDto::class.java,
        ).toWooPayload()
    }

    data class ShippingMethodDto(
        val id: String? = null,
        val title: String? = null,
    )
}
