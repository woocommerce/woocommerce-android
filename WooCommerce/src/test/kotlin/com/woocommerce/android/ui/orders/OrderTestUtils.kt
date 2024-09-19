package com.woocommerce.android.ui.orders

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.Item
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.toAppModel
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date

object OrderTestUtils {
    const val ORDER_ID = 1L
    private const val TEST_ORDER_STATUS_COUNT = 20

    fun generateOrder(
        metadata: List<WCMetaData> = emptyList(),
        paymentMethod: String = "",
        datePaid: String = "2018-02-02T16:11:13Z",
        lineItems: String = ""
    ): OrderEntity {
        return OrderEntity(
            billingFirstName = "Carissa",
            billingLastName = "King",
            currency = "USD",
            dateCreated = "2018-02-02T16:11:13Z",
            orderId = ORDER_ID,
            number = "55",
            status = "pending, Custom 1,Custom 2,Custom 3",
            total = "106.00",
            localSiteId = LocalOrRemoteId.LocalId(1),
            metaData = metadata,
            paymentMethod = paymentMethod,
            datePaid = datePaid,
            lineItems = lineItems,
        )
    }

    fun generateOrderShipmentProviders(): List<WCOrderShipmentProviderModel> {
        val result = ArrayList<WCOrderShipmentProviderModel>()
        result.add(
            WCOrderShipmentProviderModel().apply {
                localSiteId = 1
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
                localSiteId = 1
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
        return Order.getEmptyOrder(Date(), Date()).copy(
            id = orderId,
            customer = Order.Customer(
                billingAddress = Address.EMPTY.copy(
                    firstName = "Carissa",
                    lastName = "King"
                ),
                shippingAddress = Address.EMPTY
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

    fun generateOrderWithOneShipping(): OrderEntity {
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

        return OrderEntity(
            billingFirstName = "Carissa",
            billingLastName = "King",
            currency = "USD",
            dateCreated = "2018-02-02T16:11:13Z",
            localSiteId = LocalOrRemoteId.LocalId(1),
            orderId = 1,
            number = "55",
            status = "pending",
            total = "106.00",
            shippingTotal = "4.00",
            lineItems = lineItems,
            refundTotal = -BigDecimal.TEN,
            feeLines = lineItems,
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

    fun generateOrderWithCustomAmount(): OrderEntity {
        val feeLine =
            "[{\n" +
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

        return OrderEntity(
            billingFirstName = "Carissa",
            billingLastName = "King",
            currency = "USD",
            dateCreated = "2018-02-02T16:11:13Z",
            localSiteId = LocalOrRemoteId.LocalId(1),
            orderId = 1,
            number = "55",
            status = "complete",
            total = "106.00",
            shippingTotal = "10.00",
            lineItems = "",
            refundTotal = -BigDecimal.TEN,
            feeLines = feeLine,
            shippingLines = ""
        )
    }

    fun generateOrderWithMultipleShippingLines(): OrderEntity {
        return OrderEntity(
            billingFirstName = "Carissa",
            billingLastName = "King",
            currency = "USD",
            dateCreated = "2018-02-02T16:11:13Z",
            localSiteId = LocalOrRemoteId.LocalId(1),
            orderId = 1,
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
        orderId: Long = 1,
    ): List<OrderNote> {
        val result = ArrayList<OrderNote>()
        for (i in totalNotes downTo 1) {
            result.add(
                OrderNote(
                    orderId = orderId,
                    author = "",
                    isCustomerNote = false,
                    dateCreated = DateTimeUtils.dateUTCFromIso8601("2018-02-02T16:11:13Z"),
                    note = "This is a test note $i"
                )
            )
        }
        return result
    }

    fun generateTestOrderShipmentTrackings(
        totalCount: Int,
        orderId: Long = 1,
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
                    this.orderId = orderId
                    this.localSiteId = localSiteId
                }.toAppModel()
            )
        }
        return result
    }

    fun generateTestOrderItems(
        count: Int = 1,
        productId: Long = -1,
        quantity: Float = 1F
    ): List<Item> {
        val list = mutableListOf<Item>()
        for (i in 1..count) {
            list.add(
                Item(
                    itemId = i.toLong(),
                    productId = productId.takeIf { it != -1L } ?: i.toLong(),
                    name = "A test",
                    price = BigDecimal("10"),
                    sku = "",
                    quantity = quantity,
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
