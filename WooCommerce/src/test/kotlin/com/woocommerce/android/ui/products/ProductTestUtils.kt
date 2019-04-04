package com.woocommerce.android.ui.products

import org.wordpress.android.fluxc.model.WCProductModel

object ProductTestUtils {
    fun generateProduct(): WCProductModel {
        return WCProductModel(2).apply {
            dateCreated = "2018-01-05T05:14:30Z"
            localSiteId = 1
            remoteProductId = 1L
            status = "publish"
            price = "20.00"
            averageRating = "3.0"
            name = "product 1"
            description = "product 1 description"
        }
    }
}
