package org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards

import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.UNKNOWN

class LeaderboardsApiResponse : Response {
    private val rows: Array<Array<LeaderboardItemRow>>? = null
    val id: String? = null
    val label: String? = null
    val type: Type
        get() = Type.values()
                .firstOrNull { it.value == id }
                ?: UNKNOWN

    val products by lazy {
        rows
                ?.takeIf { type == PRODUCTS }
                ?.map { LeaderboardProductItem(it) }
                ?.toList()
    }

    class LeaderboardItemRow {
        val display: String? = null
        val value: String? = null
    }

    enum class Type(val value: String) {
        CUSTOMERS("customers"),
        COUPONS("coupons"),
        CATEGORIES("categories"),
        PRODUCTS("products"),
        UNKNOWN("unknown")
    }
}
