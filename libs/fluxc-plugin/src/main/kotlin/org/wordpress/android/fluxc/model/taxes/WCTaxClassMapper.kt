package org.wordpress.android.fluxc.model.taxes

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes.WCTaxRestClient.TaxClassApiResponse
import javax.inject.Inject

class WCTaxClassMapper
@Inject constructor() {
    fun map(siteId: LocalId, response: TaxClassApiResponse): WCTaxClassModel {
        return WCTaxClassModel(
            localSiteId = siteId,
            name = response.name ?: "",
            slug = response.slug ?: ""
        )
    }
}
