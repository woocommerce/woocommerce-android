package org.wordpress.android.fluxc.network.rest.wpcom.wc.metadata

import com.google.gson.JsonObject
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType
import org.wordpress.android.fluxc.model.metadata.UpdateMetadataRequest
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

internal class MetaDataRestClient @Inject internal constructor(
    private val wooNetwork: WooNetwork
) {
    suspend fun fetchMetaData(
        site: SiteModel,
        parentItemId: Long,
        parentItemType: MetaDataParentItemType
    ): WooPayload<List<WCMetaData>> {
        val path = when (parentItemType) {
            MetaDataParentItemType.ORDER -> WOOCOMMERCE.orders.id(parentItemId).pathV3
            MetaDataParentItemType.PRODUCT -> WOOCOMMERCE.products.id(parentItemId).pathV3
        }

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = path,
            params = mapOf("_fields" to "meta_data"),
            clazz = JsonObject::class.java
        )

        return response.toWooPayload {
            it.extractMetaData()
        }
    }

    suspend fun updateMetaData(
        site: SiteModel,
        request: UpdateMetadataRequest
    ): WooPayload<List<WCMetaData>> {
        val path = when (request.parentItemType) {
            MetaDataParentItemType.ORDER -> WOOCOMMERCE.orders.id(request.parentItemId).pathV3
            MetaDataParentItemType.PRODUCT -> WOOCOMMERCE.products.id(request.parentItemId).pathV3
        }

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = path,
            body = mapOf(
                "meta_data" to request.metadataChanges.toJsonArray(),
                "_fields" to "meta_data"
            ),
            clazz = JsonObject::class.java
        )

        return response.toWooPayload {
            it.extractMetaData()
        }
    }

    private fun JsonObject.extractMetaData() = getAsJsonArray("meta_data").mapNotNull {
        WCMetaData.fromJson(it)
    }
}
