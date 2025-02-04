package org.wordpress.android.fluxc.network.rest.wpcom.wc.addons

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.dto.AddOnGroupDto
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class AddOnsRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchGlobalAddOnGroups(site: SiteModel): WooPayload<List<AddOnGroupDto>> {
        val url = WOOCOMMERCE.product_add_ons.pathV1Addons

        val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                clazz = Array<AddOnGroupDto>::class.java
        )

        return response.toWooPayload { it.toList() }
    }
}
