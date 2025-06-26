package org.wordpress.android.fluxc.wc.taxes

import org.wordpress.android.fluxc.JsonLoaderUtils.jsonFileAs
import org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes.TaxRateDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes.WCTaxRestClient

object TaxTestUtils {
    fun generateSampleTaxClassApiResponse(): List<WCTaxRestClient.TaxClassApiResponse> {
        return listOf(
            WCTaxRestClient.TaxClassApiResponse("example1", "example1"),
            WCTaxRestClient.TaxClassApiResponse("example2", "example2")
        )
    }

    fun generateSampleTaxRateApiResponse() =
        "wc/tax-rate-response.json"
            .jsonFileAs(Array<TaxRateDto>::class.java)
}
