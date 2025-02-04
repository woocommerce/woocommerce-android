package org.wordpress.android.fluxc.network.rest.wpcom.wc.admin

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.utils.toMap
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class WooAdminRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {
    suspend fun getOptions(
        site: SiteModel,
        keys: List<String>
    ): WooPayload<Map<String, Any>> {
        val params = mapOf("options" to keys.joinToString(","))
        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = WOOCOMMERCE.options.pathWcAdmin,
            params = params,
            clazz = JsonElement::class.java
        ).toWooPayload { jsonElement -> jsonElement.toMap() }
    }

    suspend fun updateOptions(
        site: SiteModel,
        options: Map<String, Any>
    ): WooPayload<Unit> {
        return wooNetwork.executePostGsonRequest(
            site = site,
            path = WOOCOMMERCE.options.pathWcAdmin,
            body = options,
            clazz = Unit::class.java
        ).toWooPayload()
    }
}
