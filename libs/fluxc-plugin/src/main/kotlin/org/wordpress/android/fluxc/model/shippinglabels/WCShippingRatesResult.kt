package org.wordpress.android.fluxc.model.shippinglabels

import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient.ShippingRatesApiResponse.ShippingOption.Rate

data class WCShippingRatesResult(
    val packageRates: List<ShippingPackage>
) {
    data class ShippingOption(
        val optionId: String,
        val rates: List<Rate>
    )

    data class ShippingPackage(
        val boxId: String,
        val shippingOptions: List<ShippingOption>
    )
}
