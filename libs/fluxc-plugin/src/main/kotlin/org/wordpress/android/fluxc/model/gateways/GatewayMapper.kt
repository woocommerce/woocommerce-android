package org.wordpress.android.fluxc.model.gateways

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient.GatewayResponse
import org.wordpress.android.fluxc.persistence.entity.GatewayEntity
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

    fun toModel(entity: GatewayEntity): WCGatewayModel {
        val response = gson.fromJson(entity.data, GatewayResponse::class.java)
        return toModel(response)
    }

    fun toEntity(siteId: LocalId, response: GatewayResponse): GatewayEntity {
        val json = gson.toJson(response)
        return GatewayEntity(
            siteId = siteId,
            gatewayId = response.gatewayId,
            data = json
        )
    }

    fun toEntities(siteId: LocalId, responses: List<GatewayResponse>): List<GatewayEntity> =
        responses.map { toEntity(siteId, it) }
}
