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
        number = "51"
        status = "processing"
        total = "14.53"
    }

    val om2 = WCOrderModel(2).apply {
        billingFirstName = "Jane"
        billingLastName = "Masterson"
        currency = "CAD"
        dateCreated = "2017-12-08T16:11:13Z"
        localSiteId = 1
        number = "63"
        status = "pending"
        total = "106.00"
    }

    val om3 = WCOrderModel(2).apply {
        billingFirstName = "Mandy"
        billingLastName = "Sykes"
        currency = "USD"
        dateCreated = "2018-02-05T16:11:13Z"
        localSiteId = 1
        number = "14"
        status = "processing"
        total = "25.73"
    }

    val om4 = WCOrderModel(2).apply {
        billingFirstName = "Jennifer"
        billingLastName = "Johnson"
        currency = "CAD"
        dateCreated = "2018-02-06T09:11:13Z"
        localSiteId = 1
        number = "15"
        status = "pending, on-hold, complete"
        total = "106.00"
    }

    val om5 = WCOrderModel(2).apply {
        billingFirstName = "Christopher"
        billingLastName = "Jones"
        currency = "USD"
        dateCreated = "2018-02-05T16:11:13Z"
        localSiteId = 1
        number = "3"
        status = "pending"
        total = "106.00"
    }

    val om6 = WCOrderModel(2).apply {
        billingFirstName = "Carissa"
        billingLastName = "King"
        currency = "USD"
        dateCreated = "2018-02-02T16:11:13Z"
        localSiteId = 1
        number = "55"
        status = "pending, Custom 1,Custom 2,Custom 3"
        total = "106.00"
    }

    result.add(om1)
    result.add(om2)
    result.add(om3)
    result.add(om4)
    result.add(om5)
    result.add(om6)

    return result
}
