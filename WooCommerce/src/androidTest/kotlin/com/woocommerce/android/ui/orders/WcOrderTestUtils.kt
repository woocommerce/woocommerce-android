package com.woocommerce.android.ui.orders

import com.google.gson.Gson
import com.woocommerce.android.helpers.WCDateTimeTestUtils
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel

object WcOrderTestUtils {
    /**
     * [WCOrderModel.LineItem] values cannot be modified so adding as string here
     */
    private val MULTIPLE_PRODUCTS = Gson().toJson(
            listOf(
                    mapOf(
                            "productId" to "290",
                            "variationId" to "0",
                            "name" to "Black T-shirt",
                            "quantity" to 1,
                            "subtotal" to 10,
                            "price" to 14,
                            "total" to 15,
                            "total_tax" to 2,
                            "sku" to "blabla"
                    ),
                    mapOf(
                            "productId" to "291",
                            "variationId" to "2",
                            "name" to "White Pants",
                            "quantity" to 2,
                            "subtotal" to 12,
                            "price" to 11,
                            "total" to 13,
                            "total_tax" to 3
                    )
            )
    )

    /**
     * Generates an array containing multiple [WCOrderModel] objects.
     */
    fun generateOrders(): List<WCOrderModel> {
        val result = ArrayList<WCOrderModel>()
        val om1 = WCOrderModel(1).apply {
            // Empty first/last name
            billingFirstName = ""
            billingLastName = ""
            // Currency : USD
            currency = "USD"
            // today
            dateCreated = WCDateTimeTestUtils.formatDate(WCDateTimeTestUtils.getCurrentDateTime())
            localSiteId = 1
            number = "1"
            // Processing
            status = "Processing"
            total = "14.53"
        }

        val om2 = WCOrderModel(2).apply {
            // really long first & last name
            billingFirstName = "Itsareallylongnametoseehowitishandled"
            billingLastName = "andcontinuingwiththelongname"
            // Currency : CAD
            currency = "CAD"
            // Yesterday
            dateCreated = WCDateTimeTestUtils.formatDate(WCDateTimeTestUtils.getCurrentDateTimeMinusDays(1))
            localSiteId = 1
            number = "63"
            // Pending payment
            status = "Pending"
            total = "14.53"
        }

        val om3 = WCOrderModel(3).apply {
            billingFirstName = "Mandy"
            billingLastName = "Sykes"
            // Currency : EURO
            currency = "EUR"
            // 2 days ago
            dateCreated = WCDateTimeTestUtils.formatDate(WCDateTimeTestUtils.getCurrentDateTimeMinusDays(3))
            localSiteId = 1
            number = "14"
            // On Hold
            status = "On Hold"
            total = "14.53"
        }

        val om4 = WCOrderModel(4).apply {
            billingFirstName = "Jennifer"
            billingLastName = "Johnson"
            // Currency : INR
            currency = "INR"
            // More than a week
            dateCreated = WCDateTimeTestUtils.formatDate(WCDateTimeTestUtils.getCurrentDateTimeMinusDays(14))
            localSiteId = 1
            number = "15"
            // Completed
            status = "Completed"
            total = "14.53"
        }

        val om5 = WCOrderModel(5).apply {
            billingFirstName = "Christopher"
            billingLastName = "Jones"
            currency = "AUD"
            // Older than a month
            dateCreated = WCDateTimeTestUtils.formatDate(WCDateTimeTestUtils.getCurrentDateTimeMinusMonths(2))
            localSiteId = 1
            number = "3"
            // Cancelled
            status = "Cancelled"
            total = "14.53"
        }

        val om6 = WCOrderModel(6).apply {
            billingFirstName = "Carissa"
            billingLastName = "King"
            currency = "USD"
            dateCreated = "2018-02-02T16:11:13Z"
            localSiteId = 1
            number = "55"
            // Refunded
            status = "Refunded"
            total = "14.53"
        }

        val om7 = WCOrderModel(7).apply {
            billingFirstName = "Carissa"
            billingLastName = "King"
            currency = "USD"
            dateCreated = "2018-02-02T16:11:13Z"
            localSiteId = 1
            number = "55"
            // Failed
            status = "Failed"
            total = "14.53"
        }

        result.add(om1)
        result.add(om2)
        result.add(om3)
        result.add(om4)
        result.add(om5)
        result.add(om6)
        result.add(om7)

        return result
    }

    /**
     * Generates a single [WCOrderModel] object for Order detail screen.
     */
    fun generateOrderDetail(
        dateCreatedString: String = "",
        note: String = "",
        billingAddress1: String = "",
        billingCountry: String = "",
        billingPostalCode: String = "",
        shippingFirstName: String = "",
        shippingLastName: String = "",
        shippingAddress1: String = "",
        shippingCountry: String = "",
        billingPhone: String = "",
        products: String = MULTIPLE_PRODUCTS,
        orderStatus: String = "completed",
        shippingTotal: String = "12",
        totalTax: String = "2",
        total: String = "44",
        discountTotal: String = "",
        discountCodes: String = "",
        paymentMethodTitle: String = "",
        currency: String = "USD",
        refundTotal: Double = 0.00
    ): WCOrderModel {
        return WCOrderModel(2).apply {
            billingFirstName = "Jane"
            billingLastName = "Masterson"
            this.billingAddress1 = billingAddress1
            this.billingCountry = billingCountry
            this.currency = currency
            dateCreated = dateCreatedString
            localSiteId = 1
            this.shippingFirstName = shippingFirstName
            this.shippingLastName = shippingLastName
            this.shippingAddress1 = shippingAddress1
            this.shippingCountry = shippingCountry
            this.billingPostcode = billingPostalCode
            billingEmail = "test@testing.com"
            this.billingPhone = billingPhone
            number = "1"
            status = "pending"
            customerNote = note
            lineItems = products
            status = orderStatus
            this.shippingTotal = shippingTotal
            this.totalTax = totalTax
            this.total = total
            this.discountCodes = discountCodes
            this.discountTotal = discountTotal
            this.paymentMethodTitle = paymentMethodTitle
            this.refundTotal = refundTotal
        }
    }

    /**
     * Generates a single [WCOrderModel] object for Order detail screen.
     */
    fun generateOrderStatusDetail(status: String = "Pending"): WCOrderStatusModel {
        return WCOrderStatusModel(1).apply {
            label = status
            statusKey = status
        }
    }

    /**
     * Generates an array containing multiple [WCOrderNoteModel] objects.
     */
    fun generateSampleNotes(): List<WCOrderNoteModel> {
        val siteId = 1
        val orderId = 1
        val remoteId: Long = 1
        val result = ArrayList<WCOrderNoteModel>()
        val om1 = WCOrderNoteModel().apply {
            localSiteId = siteId
            localOrderId = orderId
            remoteNoteId = remoteId
            dateCreated = "2019-04-05T17:12:00Z"
            note = "This should be displayed first"
            isCustomerNote = true
            isSystemNote = false
        }

        val om2 = WCOrderNoteModel().apply {
            localSiteId = siteId
            localOrderId = orderId
            remoteNoteId = remoteId
            dateCreated = "2018-11-05T14:15:00Z"
            note = "This should be displayed second"
            isCustomerNote = false
            isSystemNote = true
        }

        val om3 = WCOrderNoteModel().apply {
            localSiteId = siteId
            localOrderId = orderId
            remoteNoteId = remoteId
            dateCreated = "2016-12-04T12:15:00Z"
            note = "This should be displayed third"
            isSystemNote = false
            isCustomerNote = false
        }

        result.add(om1)
        result.add(om2)
        result.add(om3)

        return result
    }

    /**
     * Generates an map containing multiple [WCOrderStatusModel] objects.
     */
    fun generateOrderStatusOptions(): Map<String, WCOrderStatusModel> {
        val options = listOf(
                WCOrderStatusModel(id = 1).apply {
                    label = "Pending Payment"
                    statusKey = "pending"
                },
                WCOrderStatusModel(id = 2).apply {
                    label = "Processing"
                    statusKey = "processing"
                },
                WCOrderStatusModel(id = 3).apply {
                    label = "On Hold"
                    statusKey = "on-hold"
                },
                WCOrderStatusModel(id = 4).apply {
                    label = "Completed"
                    statusKey = "completed"
                },
                WCOrderStatusModel(id = 5).apply {
                    label = "Cancelled"
                    statusKey = "cancelled"
                },
                WCOrderStatusModel(id = 6).apply {
                    label = "Refunded"
                    statusKey = "refunded"
                },
                WCOrderStatusModel(id = 7).apply {
                    label = "Failed"
                    statusKey = "failed"
                }
        )
        return options.map { it.statusKey to it }.toMap()
    }

    /**
     * Generates an array containing multiple [WCOrderShipmentTrackingModel] objects.
     */
    fun generateOrderShipmentTrackings(): List<WCOrderShipmentTrackingModel> {
        val result = ArrayList<WCOrderShipmentTrackingModel>()

        val om1 = WCOrderShipmentTrackingModel().apply {
            localSiteId = 1
            trackingProvider = "Anitaa Test"
            trackingNumber = "1111-1111-1111-1111"
            trackingLink = "http://somesite.com"
            dateShipped = "2018-02-02"
        }

        val om2 = WCOrderShipmentTrackingModel().apply {
            localSiteId = 1
            trackingProvider = "DHL"
            trackingNumber = "2222-2222-2222-2222"
            dateShipped = "2019-01-01"
        }

        val om3 = WCOrderShipmentTrackingModel().apply {
            localSiteId = 1
            trackingProvider = "Fedex"
            trackingNumber = "3333-3333-3333-3333"
            trackingLink = "http://testlink3.com"
            dateShipped = "2019-02-28"
        }

        val om4 = WCOrderShipmentTrackingModel().apply {
            localSiteId = 1
            trackingProvider = "Axle"
            trackingNumber = "4444"
            trackingLink = "http://testlink4.com"
            dateShipped = "2019-05-23"
        }

        result.add(om1)
        result.add(om2)
        result.add(om3)
        result.add(om4)
        return result
    }

    /**
     * Generates an array containing multiple [WCOrderShipmentProviderModel] objects.
     */
    fun generateShipmentTrackingProviderList(): List<WCOrderShipmentProviderModel> {
        val result = ArrayList<WCOrderShipmentProviderModel>()

        val pv1 = WCOrderShipmentProviderModel().apply {
            localSiteId = 1
            country = "Australia"
            carrierName = "Australia Provider 1"
            carrierLink = "http://google.com"
        }

        val pv2 = WCOrderShipmentProviderModel().apply {
            localSiteId = 1
            country = "Australia"
            carrierName = "Australia Provider 2 alongwithareallylongprovidernametocheckui"
            carrierLink = "http://google.com"
        }

        val pv3 = WCOrderShipmentProviderModel().apply {
            localSiteId = 1
            country = "India"
            carrierName = "India Provider 1"
            carrierLink = "http://google.com"
        }

        val pv4 = WCOrderShipmentProviderModel().apply {
            localSiteId = 1
            country = "United States"
            carrierName = "US Provider 1"
            carrierLink = "http://google.com"
        }

        val pv5 = WCOrderShipmentProviderModel().apply {
            localSiteId = 1
            country = "United States"
            carrierName = "US Provider 2"
            carrierLink = "http://google.com"
        }

        val pv6 = WCOrderShipmentProviderModel().apply {
            localSiteId = 1
            country = "United States"
            carrierName = "US Provider 3"
            carrierLink = "http://google.com"
        }

        result.add(pv1)
        result.add(pv2)
        result.add(pv3)
        result.add(pv4)
        result.add(pv5)
        result.add(pv6)

        return result
    }
}
