package com.woocommerce.android

import org.wordpress.android.fluxc.model.WCOrderModel

/**
 * Generates an array containing multiple [WCOrderModel] objects.
 */
fun generateWCOrderModels(): List<WCOrderModel> {
    val result = ArrayList<WCOrderModel>()
    val om1 = WCOrderModel(1).apply {
        billingFirstName = "John"
        billingLastName = "Peters"
        currency = "USD"
        dateCreated = "2018-01-05T05:14:30Z"
        localSiteId = 1
        remoteOrderId = 51
        status = "processing"
        total = 14.53F
    }

    val om2 = WCOrderModel(2).apply {
        billingFirstName = "Jane"
        billingLastName = "Masterson"
        currency = "CAD"
        dateCreated = "2017-12-08T16:11:13Z"
        localSiteId = 1
        remoteOrderId = 63
        status = "pending"
        total = 106.0F
    }

    val om3 = WCOrderModel(2).apply {
        billingFirstName = "Mandy"
        billingLastName = "Sykes"
        currency = "USD"
        dateCreated = "2018-02-05T16:11:13Z"
        localSiteId = 1
        remoteOrderId = 14
        status = "processing"
        total = 25.73F
    }

    val om4 = WCOrderModel(2).apply {
        billingFirstName = "Jennifer"
        billingLastName = "Johnson"
        currency = "CAD"
        dateCreated = "2018-02-06T09:11:13Z"
        localSiteId = 1
        remoteOrderId = 15
        status = "pending"
        total = 106.0F
    }

    val om5 = WCOrderModel(2).apply {
        billingFirstName = "Christopher"
        billingLastName = "Jones"
        currency = "USD"
        dateCreated = "2018-02-05T16:11:13Z"
        localSiteId = 1
        remoteOrderId = 3
        status = "pending"
        total = 106.0F
    }

    val om6 = WCOrderModel(2).apply {
        billingFirstName = "Carissa"
        billingLastName = "King"
        currency = "USD"
        dateCreated = "2018-02-02T16:11:13Z"
        localSiteId = 1
        remoteOrderId = 55
        status = "pending"
        total = 106.0F
    }

    result.add(om1)
    result.add(om2)
    result.add(om3)
    result.add(om4)
    result.add(om5)
    result.add(om6)

    return result
}
