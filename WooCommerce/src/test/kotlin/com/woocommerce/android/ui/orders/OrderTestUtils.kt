package com.woocommerce.android.ui.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.toAppModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date

object OrderTestUtils {
    const val TEST_LOCAL_SITE_ID = 1
    const val TEST_ORDER_STATUS_COUNT = 20

    /**
     * Generates an array containing multiple [WCOrderModel] objects.
     */
    fun generateOrders(): List<WCOrderModel> {
        val result = ArrayList<WCOrderModel>()
        val om1 = WCOrderModel(1).apply {
            billingFirstName = "John"
            billingLastName = "Peters"
            currency = "USD"
            dateCreated = "2018-01-05T05:14:30Z"
            localSiteId = TEST_LOCAL_SITE_ID
            number = "51"
            status = "processing"
            total = "14.53"
        }

        val om2 = WCOrderModel(2).apply {
            billingFirstName = "Jane"
            billingLastName = "Masterson"
            currency = "CAD"
            dateCreated = "2017-12-08T16:11:13Z"
            localSiteId = TEST_LOCAL_SITE_ID
            number = "63"
            status = "pending"
            total = "106.00"
        }

        val om3 = WCOrderModel(2).apply {
            billingFirstName = "Mandy"
            billingLastName = "Sykes"
            currency = "USD"
            dateCreated = "2018-02-05T16:11:13Z"
            localSiteId = TEST_LOCAL_SITE_ID
            number = "14"
            status = "processing"
            total = "25.73"
        }

        val om4 = WCOrderModel(2).apply {
            billingFirstName = "Jennifer"
            billingLastName = "Johnson"
            currency = "CAD"
            dateCreated = "2018-02-06T09:11:13Z"
            localSiteId = TEST_LOCAL_SITE_ID
            number = "15"
            status = "pending, on-hold, complete"
            total = "106.00"
        }

        val om5 = WCOrderModel(2).apply {
            billingFirstName = "Christopher"
            billingLastName = "Jones"
            currency = "USD"
            dateCreated = "2018-02-05T16:11:13Z"
            localSiteId = TEST_LOCAL_SITE_ID
            number = "3"
            status = "pending"
            total = "106.00"
        }

        val om6 = WCOrderModel(2).apply {
            billingFirstName = "Carissa"
            billingLastName = "King"
            currency = "USD"
            dateCreated = "2018-02-02T16:11:13Z"
            localSiteId = TEST_LOCAL_SITE_ID
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

    fun generateOrder(): WCOrderModel {
        return WCOrderModel(2).apply {
            billingFirstName = "Carissa"
            billingLastName = "King"
            currency = "USD"
            dateCreated = "2018-02-02T16:11:13Z"
            localSiteId = TEST_LOCAL_SITE_ID
            number = "55"
            status = "pending, Custom 1,Custom 2,Custom 3"
            total = "106.00"
        }
    }

    fun generateOrderNotes(totalNotes: Int, lOrderId: Int, lSiteId: Int): List<WCOrderNoteModel> {
        val result = ArrayList<WCOrderNoteModel>()

        for (i in totalNotes downTo 1) {
            result.add(WCOrderNoteModel(totalNotes).apply {
                isCustomerNote = false
                dateCreated = "2018-02-02T16:11:13Z"
                localOrderId = lOrderId
                localSiteId = lSiteId
                note = "This is a test note $i"
            })
        }
        return result
    }

    fun generateOrderShipmentTrackings(totalCount: Int, lOrderId: Int): List<WCOrderShipmentTrackingModel> {
        val result = ArrayList<WCOrderShipmentTrackingModel>()
        for (i in totalCount downTo 1) {
            result.add(WCOrderShipmentTrackingModel(totalCount).apply {
                trackingProvider = "TNT Express $i"
                trackingNumber = "$i"
                dateShipped = SimpleDateFormat("yyyy-MM-dd").format(Date())
                trackingLink = "www.somelink$i.com"
                localOrderId = lOrderId
            })
        }
        return result
    }

    fun generateOrderShipmentProviders(): List<WCOrderShipmentProviderModel> {
        val result = ArrayList<WCOrderShipmentProviderModel>()
        result.add(WCOrderShipmentProviderModel().apply {
            localSiteId = TEST_LOCAL_SITE_ID
            country = "Australia"
            carrierName = "Anitaa Test"
            carrierLink = "http://google.com"
        })
        return result
    }

    @Suppress("WeakerAccess")
    fun generateOrderStatusOptions(): List<WCOrderStatusModel> {
        return CoreOrderStatus.values().map {
            WCOrderStatusModel().apply {
                localSiteId = TEST_LOCAL_SITE_ID
                statusKey = it.value
                label = it.value
                statusCount = TEST_ORDER_STATUS_COUNT
            }
        }
    }

    fun generateOrderStatusOptionsMappedByStatus(): Map<String, WCOrderStatusModel> =
            generateOrderStatusOptions().map { it.statusKey to it }.toMap()

    fun generateShippingLabel(localSiteId: Int = 1, remoteOrderId: Long, shippingLabelId: Long): ShippingLabel {
        return WCShippingLabelModel().apply {
            this.localSiteId = localSiteId
            localOrderId = remoteOrderId
            remoteShippingLabelId = shippingLabelId
            packageName = "Package"
            serviceName = "Service"
            dateCreated = Date().time.toString()
        }.toAppModel()
    }

    fun generateShippingLabels(totalCount: Int = 5, orderIdentifier: OrderIdentifier): List<ShippingLabel> {
        val result = ArrayList<ShippingLabel>()
        for (i in totalCount downTo 1) {
            result.add(WCShippingLabelModel().apply {
                localSiteId = orderIdentifier.toIdSet().localSiteId
                localOrderId = orderIdentifier.toIdSet().id.toLong()
                remoteShippingLabelId = i.toLong()
                packageName = "Package$i"
                serviceName = "Service$i"
                dateCreated = Date().time.toString()
            }.toAppModel())
        }
        return result
    }

    fun generateRefunds(totalCount: Int = 5): List<Refund> {
        val result = ArrayList<Refund>()
        for (i in totalCount downTo 1) {
            result.add(Refund(
                id = i.toLong(),
                amount = i.toBigDecimal(),
                dateCreated = Date(),
                reason = "Test",
                automaticGatewayRefund = true,
                items = listOf(
                    Refund.Item(
                        productId = 15,
                        quantity = 1,
                        id = 1L,
                        name = "A test",
                        variationId = 0,
                        subtotal = BigDecimal.valueOf(10.00),
                        total = BigDecimal.valueOf(10.00),
                        totalTax = BigDecimal.ZERO,
                        price = BigDecimal.valueOf(10.00)
                    )
                ),
                shippingLines = listOf(
                    Refund.ShippingLine(
                        itemId = 42,
                        methodId = "flat_rate",
                        methodTitle = "DHL",
                        total = BigDecimal.valueOf(13.00),
                        totalTax = BigDecimal.valueOf(3.00)
                    )
                )
            ))
        }
        return result
    }

    fun generateTestOrder(orderIdentifier: OrderIdentifier = "1-1-1"): Order {
        val orderIdSet = orderIdentifier.toIdSet()
        return WCOrderModel(orderIdSet.id).apply {
            billingFirstName = "Carissa"
            billingLastName = "King"
            currency = "USD"
            dateCreated = "2018-02-02T16:11:13Z"
            localSiteId = orderIdSet.localSiteId
            remoteOrderId = orderIdSet.remoteOrderId
            number = "55"
            status = "pending"
            total = "106.00"
            lineItems = "[{\n" +
                "    \"id\":1,\n" +
                "    \"name\":\"A test\",\n" +
                "    \"product_id\":15,\n" +
                "    \"quantity\":1,\n" +
                "    \"tax_class\":\"\",\n" +
                "    \"subtotal\":\"10.00\",\n" +
                "    \"subtotal_tax\":\"0.00\",\n" +
                "    \"total\":\"10.00\",\n" +
                "    \"total_tax\":\"0.00\",\n" +
                "    \"taxes\":[],\n" +
                "    \"meta_data\":[],\n" +
                "    \"sku\":null,\n" +
                "    \"price\":10\n" +
                "  }]"
            refundTotal = -10.0
        }.toAppModel()
    }

    fun generateOrderWithFee(orderIdentifier: OrderIdentifier = "1-1-1"): WCOrderModel {
        val orderIdSet = orderIdentifier.toIdSet()
        return WCOrderModel(orderIdSet.id).apply {
            billingFirstName = "Carissa"
            billingLastName = "King"
            currency = "USD"
            dateCreated = "2018-02-02T16:11:13Z"
            localSiteId = orderIdSet.localSiteId
            remoteOrderId = orderIdSet.remoteOrderId
            number = "55"
            status = "pending"
            total = "106.00"
            shippingTotal = "4.00"
            lineItems = "[{\n" +
                "    \"id\":1,\n" +
                "    \"name\":\"A test\",\n" +
                "    \"product_id\":15,\n" +
                "    \"quantity\":1,\n" +
                "    \"tax_class\":\"\",\n" +
                "    \"subtotal\":\"10.00\",\n" +
                "    \"subtotal_tax\":\"0.00\",\n" +
                "    \"total\":\"10.00\",\n" +
                "    \"total_tax\":\"0.00\",\n" +
                "    \"taxes\":[],\n" +
                "    \"meta_data\":[],\n" +
                "    \"sku\":null,\n" +
                "    \"price\":10\n" +
                "  }]"
            refundTotal = -10.0
            feeLines = lineItems
//                "[{\n" +
//                "    \"name\":\"A fee\",\n" +
//                "    \"total\":\"10.00\",\n" +
//                "  }]"
            shippingLines =
                "[{" +
                    "\"id\":119,\n" +
                    "   \"method_title\":\"Shipping\",\n" +
                    "   \"method_id\":\"free_shipping\",\n" +
                    "   \"instance_id\":\"0\",\n" +
                    "   \"total\":\"30.00\",\n" +
                    "   \"total_tax\":\"0.00\",\n" +
                    "   \"taxes\":[],\n" +
                    "   \"meta_data\":[]}]"
        }
    }

    fun generateOrderWithMultipleShippingLines(orderIdentifier: OrderIdentifier = "1-1-1"): WCOrderModel {
        val orderIdSet = orderIdentifier.toIdSet()

        return WCOrderModel(orderIdSet.id).apply {
            billingFirstName = "Carissa"
            billingLastName = "King"
            currency = "USD"
            dateCreated = "2018-02-02T16:11:13Z"
            localSiteId = orderIdSet.localSiteId
            remoteOrderId = orderIdSet.remoteOrderId
            number = "55"
            status = "pending"
            total = "106.00"
            totalTax = "0.00"
            shippingTotal = "4.00"
            lineItems = "[{\n" +
                "    \"id\":1,\n" +
                "    \"name\":\"A test\",\n" +
                "    \"product_id\":15,\n" +
                "    \"quantity\":1,\n" +
                "    \"tax_class\":\"\",\n" +
                "    \"subtotal\":\"10.00\",\n" +
                "    \"subtotal_tax\":\"0.00\",\n" +
                "    \"total\":\"10.00\",\n" +
                "    \"total_tax\":\"0.00\",\n" +
                "    \"taxes\":[],\n" +
                "    \"meta_data\":[],\n" +
                "    \"sku\":null,\n" +
                "    \"price\":10\n" +
                "  }]"
            refundTotal = -10.0
            shippingLines =
                "[{" +
                    "\"id\":119,\n" +
                "   \"method_title\":\"Shipping\",\n" +
                "   \"method_id\":\"free_shipping\",\n" +
                "   \"instance_id\":\"0\",\n" +
                "   \"total\":\"30.00\",\n" +
                "   \"total_tax\":\"0.00\",\n" +
                "   \"taxes\":[],\n" +
                "   \"meta_data\":[]},\n" +
                "{  " +
                    "\"id\":120,\n" +
                "   \"method_title\":\"Shipping Two\",\n" +
                "   \"method_id\":\"\",\n" +
                "   \"instance_id\":\"0\",\n" +
                "   \"total\":\"20.00\",\n" +
                "   \"total_tax\":\"0.00\",\n" +
                "   \"taxes\":[],\n" +
                "   \"meta_data\":[]\n" +
                "}]"
        }
    }

    fun generateTestOrderNotes(
        totalNotes: Int,
        orderIdentifier: OrderIdentifier = "1-1-1"
    ): List<OrderNote> {
        val orderIdSet = orderIdentifier.toIdSet()

        val result = ArrayList<OrderNote>()
        for (i in totalNotes downTo 1) {
            result.add(WCOrderNoteModel(totalNotes).apply {
                isCustomerNote = false
                dateCreated = "2018-02-02T16:11:13Z"
                localOrderId = orderIdSet.id
                localSiteId = orderIdSet.localSiteId
                note = "This is a test note $i"
            }.toAppModel())
        }
        return result
    }

    fun generateTestOrderShipmentTrackings(
        totalCount: Int,
        orderIdentifier: OrderIdentifier = "1-1-1"
    ): List<OrderShipmentTracking> {
        val orderIdSet = orderIdentifier.toIdSet()
        val result = ArrayList<OrderShipmentTracking>()
        for (i in totalCount downTo 1) {
            result.add(WCOrderShipmentTrackingModel(totalCount).apply {
                trackingProvider = "TNT Express $i"
                trackingNumber = "$i"
                dateShipped = SimpleDateFormat("yyyy-MM-dd").format(Date())
                trackingLink = "www.somelink$i.com"
                localOrderId = orderIdSet.id
                localSiteId = orderIdSet.localSiteId
            }.toAppModel())
        }
        return result
    }
}
