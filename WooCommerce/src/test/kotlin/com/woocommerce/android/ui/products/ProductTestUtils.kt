package com.woocommerce.android.ui.products

import org.wordpress.android.fluxc.model.WCProductModel

object ProductTestUtils {
    fun generateProducts(): List<WCProductModel> {
        val pm1 = generateProduct()

        val pm2 = WCProductModel(1).apply {
            dateCreated = "2018-01-05T05:14:30Z"
            localSiteId = 1
            remoteProductId = 2L
            status = "publish"
            price = "10.00"
            averageRating = "5.0"
            name = "product 2"
            description = "product 2 description"
        }

        val pm3 = WCProductModel(1).apply {
            dateCreated = "2018-01-05T05:14:30Z"
            localSiteId = 1
            remoteProductId = 2L
            status = "publish"
            price = "10.00"
            averageRating = "0.0"
            name = "product 3"
            description = "product 3 description"
        }

        val result = ArrayList<WCProductModel>()
        result.add(pm1)
        result.add(pm2)
        result.add(pm3)
        return result
    }

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
