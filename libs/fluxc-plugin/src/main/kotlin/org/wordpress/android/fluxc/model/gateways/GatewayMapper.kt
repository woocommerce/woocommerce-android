package org.wordpress.android.fluxc.model.gateways

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient.GatewayResponse
import org.wordpress.android.fluxc.persistence.WCGatewaySqlUtils.GatewaysTable
import javax.inject.Inject

class GatewayMapper @Inject constructor(private val gson: Gson) {
    fun toModel(response: GatewayResponse): WCGatewayModel {
        return WCGatewayModel(
            response.gatewayId,
            response.title ?: "",
            response.description ?: "",
            response.order?.toIntOrNull() ?: 0,
            response.enabled ?: false,
            response.methodTitle ?: "",
            response.methodDescription ?: "",
            response.features ?: emptyList()
        )
    }

    fun toModel(entity: GatewaysTable): WCGatewayModel {
        val response = gson.fromJson(entity.data, GatewayResponse::class.java)
        return toModel(response)
    }

    fun toEntity(siteId: LocalId, response: GatewayResponse): GatewaysTable {
        val json = gson.toJson(response)
        return GatewaysTable(
            localSiteId = siteId.value,
            gatewayId = response.gatewayId,
            data = json
        )
    }

    fun toEntities(siteId: LocalId, responses: List<GatewayResponse>): List<GatewaysTable> =
        responses.map { toEntity(siteId, it) }
}
