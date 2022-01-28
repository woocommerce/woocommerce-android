package com.woocommerce.android.ui.orders

import com.woocommerce.android.model.*
import com.woocommerce.android.model.Order.Item
import org.wordpress.android.fluxc.model.*
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object OrderTestUtils {
    val TEST_LOCAL_SITE_ID = LocalOrRemoteId.LocalId(1)
    val TEST_REMOTE_ORDER_ID = LocalOrRemoteId.RemoteId(2)
    const val TEST_ORDER_STATUS_COUNT = 20

    /**
     * Generates an array containing multiple [WCOrderModel] objects.
     */
    fun generateOrders(): List<WCOrderModel> {
        val result = ArrayList<WCOrderModel>()
        val om1 = WCOrderModel(
            id = 1,
            billingFirstName = "John",
            billingLastName = "Peters",
            currency = "USD",
            dateCreated = "2018-01-05T05:14:30Z",
            localSiteId = TEST_LOCAL_SITE_ID,
            remoteOrderId = TEST_REMOTE_ORDER_ID,
            number = "51",
            status = "processing",
            total = "14.53",
        )

        val om2 = WCOrderModel(
            id = 2,
            billingFirstName = "Jane",
            billingLastName = "Masterson",
            currency = "CAD",
            dateCreated = "2017-12-08T16:11:13Z",
            localSiteId = TEST_LOCAL_SITE_ID,
            remoteOrderId = TEST_REMOTE_ORDER_ID,
            number = "63",
            status = "pending",
            total = "106.00",
        )

        val om3 = WCOrderModel(
            id = 2,
            billingFirstName = "Mandy",
            billingLastName = "Sykes",
            currency = "USD",
            dateCreated = "2018-02-05T16:11:13Z",
            localSiteId = TEST_LOCAL_SITE_ID,
            remoteOrderId = TEST_REMOTE_ORDER_ID,
            number = "14",
            status = "processing",
            total = "25.73",
        )

        val om4 = WCOrderModel(
            id = 2,
            billingFirstName = "Jennifer",
            billingLastName = "Johnson",
            currency = "CAD",
            dateCreated = "2018-02-06T09:11:13Z",
            localSiteId = TEST_LOCAL_SITE_ID,
            remoteOrderId = TEST_REMOTE_ORDER_ID,
            number = "15",
            status = "pending, on-hold, complete",
            total = "106.00",
        )

        val om5 = WCOrderModel(
            id = 2,
            billingFirstName = "Christopher",
            billingLastName = "Jones",
            currency = "USD",
            dateCreated = "2018-02-05T16:11:13Z",
            localSiteId = TEST_LOCAL_SITE_ID,
            remoteOrderId = TEST_REMOTE_ORDER_ID,
            number = "3",
            status = "pending",
            total = "106.00",
        )

        val om6 = WCOrderModel(
            id = 2,
            billingFirstName = "Carissa",
            billingLastName = "King",
            currency = "USD",
            dateCreated = "2018-02-02T16:11:13Z",
            localSiteId = TEST_LOCAL_SITE_ID,
            remoteOrderId = TEST_REMOTE_ORDER_ID,
            number = "55",
            status = "pending, Custom 1,Custom 2,Custom 3",
            total = "106.00",
        )

        result.add(om1)
        result.add(om2)
        result.add(om3)
        result.add(om4)
        result.add(om5)
        result.add(om6)

        return result
    }

    fun generateOrder(): WCOrderModel {
        return WCOrderModel(
            id = 2,
            billingFirstName = "Carissa",
            billingLastName = "King",
            currency = "USD",
            dateCreated = "2018-02-02T16:11:13Z",
            localSiteId = TEST_LOCAL_SITE_ID,
            remoteOrderId = TEST_REMOTE_ORDER_ID,
            number = "55",
            status = "pending, Custom 1,Custom 2,Custom 3",
            total = "106.00",
        )
    }

    fun generateOrderNotes(totalNotes: Int, lOrderId: Int, lSiteId: Int): List<WCOrderNoteModel> {
        val result = ArrayList<WCOrderNoteModel>()

        for (i in totalNotes downTo 1) {
            result.add(
                WCOrderNoteModel(totalNotes).apply {
                    isCustomerNote = false
                    dateCreated = "2018-02-02T16:11:13Z"
                    localOrderId = lOrderId
                    localSiteId = lSiteId
                    note = "This is a test note $i"
                }
            )
        }
        return result
    }

    fun generateOrderShipmentTrackings(totalCount: Int, lOrderId: Int): List<WCOrderShipmentTrackingModel> {
        val result = ArrayList<WCOrderShipmentTrackingModel>()
        for (i in totalCount downTo 1) {
            result.add(
                WCOrderShipmentTrackingModel(totalCount).apply {
                    trackingProvider = "TNT Express $i"
                    trackingNumber = "$i"
                    dateShipped = SimpleDateFormat("yyyy-MM-dd").format(Date())
                    trackingLink = "www.somelink$i.com"
                    localOrderId = lOrderId
                }
            )
        }
        return result
    }

    fun generateOrderShipmentProviders(): List<WCOrderShipmentProviderModel> {
        val result = ArrayList<WCOrderShipmentProviderModel>()
        result.add(
            WCOrderShipmentProviderModel().apply {
                localSiteId = TEST_LOCAL_SITE_ID.value
                country = "Australia"
                carrierName = "Anitaa Test"
                carrierLink = "http://google.com"
            }
        )
        return result
    }

    @Suppress("WeakerAccess")
    fun generateOrderStatusOptions(): List<WCOrderStatusModel> {
        return CoreOrderStatus.values().map {
            WCOrderStatusModel().apply {
                localSiteId = TEST_LOCAL_SITE_ID.value
                statusKey = it.value
                label = it.value
                statusCount = TEST_ORDER_STATUS_COUNT
            }
        }
    }

    fun generateOrderStatusOptionsMappedByStatus(): Map<String, WCOrderStatusModel> =
        generateOrderStatusOptions().map { it.statusKey to it }.toMap()

    fun generateShippingLabel(shippingLabelId: Long): ShippingLabel {
        return ShippingLabel(
            id = shippingLabelId,
            packageName = "Package",
            serviceName = "Service",
            createdDate = Date(),
            commercialInvoiceUrl = "",
        )
    }

    fun generateShippingLabels(totalCount: Int = 5): List<ShippingLabel> {
        val result = ArrayList<ShippingLabel>()
        for (i in totalCount downTo 1) {
            result.add(
                ShippingLabel(
                    id = i.toLong(),
                    packageName = "Package$i",
                    serviceName = "Service$i",
                    createdDate = Date(),
                    commercialInvoiceUrl = "",
                )
            )
        }
        return result
    }

    fun generateRefunds(totalCount: Int = 5): List<Refund> {
        val result = ArrayList<Refund>()
        for (i in totalCount downTo 1) {
            result.add(
                Refund(
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
                            price = BigDecimal.valueOf(10.00),
                            orderItemId = 1L
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
                    ),
                    feeLines = listOf(
                        Refund.FeeLine(
                            id = 30,
                            name = "$30 fee",
                            total = BigDecimal.valueOf(399.00),
                            totalTax = BigDecimal.valueOf(39.90)
                        )
                    )
                )
            )
        }
        return result
    }

    fun generateTestOrder(orderId: Long = 1): Order {
        return Order.EMPTY.copy(
            id = orderId,
            billingAddress = Address.EMPTY.copy(
                firstName = "Carissa",
                lastName = "King"
            ),
            currency = "USD",
            dateCreated = DateTimeUtils.dateUTCFromIso8601("2018-02-02T16:11:13Z"),
            number = "55",
            status = Order.Status.Pending,
            total = BigDecimal("106.00"),
            items = generateTestOrderItems(productId = 15),
            refundTotal = -BigDecimal.TEN,
        )
    }

    fun generateOrderWithFee(): WCOrderModel {
        val lineItems = "[{\n" +
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

        return WCOrderModel(
            id = 1,
            billingFirstName = "Carissa",
            billingLastName = "King",
            currency = "USD",
            dateCreated = "2018-02-02T16:11:13Z",
            localSiteId = LocalOrRemoteId.LocalId(1),
            remoteOrderId = LocalOrRemoteId.RemoteId(1),
            number = "55",
            status = "pending",
            total = "106.00",
            shippingTotal = "4.00",
            lineItems = lineItems,
            refundTotal = -BigDecimal.TEN,
            feeLines = lineItems,
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
                "   \"meta_data\":[]}]",
        )
    }

    fun generateOrderWithMultipleShippingLines(): WCOrderModel {
        return WCOrderModel(
            id = 1,
            billingFirstName = "Carissa",
            billingLastName = "King",
            currency = "USD",
            dateCreated = "2018-02-02T16:11:13Z",
            localSiteId = LocalOrRemoteId.LocalId(1),
            remoteOrderId = LocalOrRemoteId.RemoteId(1),
            number = "55",
            status = "pending",
            total = "106.00",
            totalTax = "0.00",
            shippingTotal = "4.00",
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
                "  }]",
            refundTotal = -BigDecimal.TEN,
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
                "}]",
        )
    }

    fun generateTestOrderNotes(
        totalNotes: Int,
        localOrderId: Int = 1,
        localSiteId: Int = 1
    ): List<OrderNote> {
        val result = ArrayList<OrderNote>()
        for (i in totalNotes downTo 1) {
            result.add(
                WCOrderNoteModel(totalNotes).apply {
                    this.isCustomerNote = false
                    this.dateCreated = "2018-02-02T16:11:13Z"
                    this.localOrderId = localOrderId
                    this.localSiteId = localSiteId
                    this.note = "This is a test note $i"
                }.toAppModel()
            )
        }
        return result
    }

    fun generateTestOrderShipmentTrackings(
        totalCount: Int,
        localOrderId: Int = 1,
        localSiteId: Int = 1
    ): List<OrderShipmentTracking> {
        val result = ArrayList<OrderShipmentTracking>()
        for (i in totalCount downTo 1) {
            result.add(
                WCOrderShipmentTrackingModel(totalCount).apply {
                    this.trackingProvider = "TNT Express $i"
                    this.trackingNumber = "$i"
                    this.dateShipped = SimpleDateFormat("yyyy-MM-dd").format(Date())
                    this.trackingLink = "www.somelink$i.com"
                    this.localOrderId = localOrderId
                    this.localSiteId = localSiteId
                }.toAppModel()
            )
        }
        return result
    }

    fun generateTestOrderItems(
        count: Int = 1,
        productId: Long = -1
    ): List<Item> {
        val list = mutableListOf<Item>()
        for (i in 1..count) {
            list.add(
                Order.Item(
                    itemId = i.toLong(),
                    productId = productId.takeIf { it != -1L } ?: i.toLong(),
                    name = "A test",
                    price = BigDecimal("10"),
                    sku = "",
                    quantity = 1f,
                    subtotal = BigDecimal("10"),
                    totalTax = BigDecimal.ZERO,
                    total = BigDecimal("10"),
                    variationId = 0,
                    attributesList = emptyList()
                )
            )
        }
        return list
    }
}
