package org.wordpress.android.fluxc.model.taxes

import org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes.WCTaxRestClient.TaxClassApiResponse
import javax.inject.Inject

class WCTaxClassMapper
@Inject constructor() {
    fun map(response: TaxClassApiResponse): WCTaxClassModel {
        return WCTaxClassModel().apply {
            name = response.name ?: ""
            slug = response.slug ?: ""
        }
    }
}
