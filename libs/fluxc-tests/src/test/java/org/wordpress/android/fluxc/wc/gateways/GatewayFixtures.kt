package org.wordpress.android.fluxc.wc.gateways

import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient.GatewayResponse
import org.wordpress.android.fluxc.persistence.WCGatewaySqlUtils.GatewaysTable

val GATEWAYS_RESPONSE = listOf(
        GatewayResponse(
            "cod",
            "Cash on Delivery",
            "Pay cash",
            "",
            false,
                "COD: Method title",
                "COD: Method description",
            listOf("products")
        ),
        GatewayResponse(
                "stripe",
                "Credit card",
                "Pay with CC",
                "1",
                true,
                "Stripe: Method title",
                "Stripe: Method description",
                listOf("products", "refunds")
        )
)

val GATEWAYS_ENTITIES = listOf(
    GatewaysTable(
        localSiteId = 2,
        gatewayId = "cod",
        data = """
                {
                    "description":"Pay cash",
                    "enabled":false,
                    "method_supports":["products"],
                    "id":"cod",
                    "method_description":"COD: Method description",
                    "method_title":"COD: Method title",
                    "order":"0",
                    "title":"Cash on Delivery"
                }
                """
    ),
    GatewaysTable(
        localSiteId = 2,
        gatewayId = "stripe",
        data = """
                {
                    "description":"Pay with CC",
                    "enabled":true,
                    "method_supports":["products","refunds"],
                    "id":"stripe",
                    "method_description":"Stripe: Method description",
                    "method_title":"Stripe: Method title",
                    "order":"1",
                    "title":"Credit card"
                }
                """
    )
)
