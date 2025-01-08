package com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooShippingRatesRestClient @Inject constructor(
    private val wooNetwork: WooNetwork,
    private val gson: Gson
) {
    suspend fun getShippingRates(
        site: SiteModel,
        orderId: String,
        origin: OriginAddressDTO,
        destination: DestinationAddressDTO,
        packages: List<PackageDTO>
    ): WooResult<Map<String, Map<String, WooShippingRatesDTO>>> {
        val body = mapOf(
            "order_id" to orderId,
            "origin" to origin,
            "destination" to destination,
            "packages" to packages
        )

        return wooNetwork.executePostGsonRequest(
            site = site,
            path = URL,
            body = body,
            clazz = JsonObject::class.java,
        ).toWooPayload().asWooResult { json ->
            val type = object : TypeToken<Map<String, Map<String, WooShippingRatesDTO>>>() {}.type
            val response = gson.fromJson<Map<String, Map<String, WooShippingRatesDTO>>>(json, type)
            response
        }
    }

    companion object {
        private const val URL = "/wcshipping/v1/label/rate"
    }
}
