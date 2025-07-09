package org.wordpress.android.fluxc.wc.refunds

import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient.RefundResponse
import org.wordpress.android.fluxc.persistence.entity.RefundEntity

val REFUND_RESPONSE = RefundResponse(
        1L,
        "2011-10-10T14:48:00",
        "10.00",
        "No reason",
        false,
        emptyList(),
        emptyList(),
        emptyList()
)

val TEST_SITE = SiteModel().apply { id = 2 }

val REFUND_ENTITY = RefundEntity(
    localSiteId = TEST_SITE.localId(),
    orderId = LocalOrRemoteId.RemoteId(1L),
    refundId = LocalOrRemoteId.RemoteId(1L),
    data = "test-data"
)
