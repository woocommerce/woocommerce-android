package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats

import org.wordpress.android.fluxc.network.Response

data class VisitorStatsSummaryApiResponse(
    val date: String? = null,
    val period: String? = null,
    val views: Int = 0,
    val visitors: Int = 0
) : Response
