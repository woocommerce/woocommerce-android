package org.wordpress.android.fluxc.wc.gateways

import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient.GatewayResponse

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
