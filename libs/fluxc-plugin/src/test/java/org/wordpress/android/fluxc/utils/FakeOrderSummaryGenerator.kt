package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCOrderSummaryModel

object FakeOrderSummaryGenerator {

    fun IntProgression.asOrderSummaries(siteId: LocalId): List<WCOrderSummaryModel> {
        return this.map {
            createOrderSummary(siteId, RemoteId(it.toLong()))
        }
    }

    private fun createOrderSummary(siteId: LocalId, orderId: RemoteId): WCOrderSummaryModel {
        return WCOrderSummaryModel(
            siteId = siteId,
            orderId = orderId,
            dateCreated = "2023-01-01T10:00:00Z"
        ).apply {
            dateModified = "2023-01-01T10:00:00Z"
        }
    }
}
