package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ShipmentItem
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class GetShipmentsTests : BaseUnitTest() {
    private val wooShippingLabelRepository: WooShippingLabelRepository = mock {
        on { getLabels(any()) } doReturn emptyList()
        on { getShipments(any()) } doReturn emptyMap()
    }
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val productDetailRepository: ProductDetailRepository = mock()

    private val sut = GetShipments(wooShippingLabelRepository, orderDetailRepository, productDetailRepository)

    @Test
    fun `when order only contains refunded products then should return empty list`() = testBlocking {
        val productId = 18L
        val quantity = 2F
        val refunds = OrderTestUtils.generateItemsRefunds(listOf(Pair(productId, quantity.toInt())))
        val order = OrderTestUtils.generateTestOrder().copy(
            items = OrderTestUtils.generateTestOrderItems(productId = productId, quantity = quantity)
        )

        whenever(orderDetailRepository.getOrderRefunds(eq(order.id))) doReturn refunds

        val result = sut.invoke(order)
        val items = result.first().items

        assertTrue(items.isEmpty())
        verify(productDetailRepository, never()).getProduct(any())
    }

    @Test
    fun `when order don't contain refunded products then should return expected list`() = testBlocking {
        val itemsSize = 5
        val refunds = emptyList<Refund>()
        val order = OrderTestUtils.generateTestOrder().copy(
            items = OrderTestUtils.generateTestOrderItems(count = itemsSize)
        )
        whenever(orderDetailRepository.getOrderRefunds(eq(order.id))) doReturn refunds
        whenever(productDetailRepository.getProduct(any())).thenAnswer { invocation ->
            val productId = invocation.arguments[0] as Long
            ProductTestUtils.generateProduct(productId = productId, productName = "Product $productId")
        }

        val result = sut.invoke(order)
        val items = result.first().items

        assertTrue(items.isNotEmpty())
        assertEquals(items.size, itemsSize)
    }

    @Test
    fun `when refunded quantity is less than product quantity then should return expected list`() = testBlocking {
        val productId = 18L
        val quantity = 2F
        val itemsSize = 1
        val refunds = OrderTestUtils.generateItemsRefunds(listOf(Pair(productId, quantity.toInt() - 1)))
        val order = OrderTestUtils.generateTestOrder().copy(
            items = OrderTestUtils.generateTestOrderItems(count = itemsSize, productId = productId, quantity = quantity)
        )

        whenever(orderDetailRepository.getOrderRefunds(eq(order.id))) doReturn refunds
        whenever(productDetailRepository.getProduct(any())).thenAnswer { invocation ->
            val id = invocation.arguments[0] as Long
            ProductTestUtils.generateProduct(productId = id, productName = "Product $id")
        }

        val result = sut.invoke(order)
        val items = result.first().items

        assertTrue(items.isNotEmpty())
        assertEquals(items.size, itemsSize)
    }

    @Test
    fun `when there are virtual or sample products then those products are filtered from the result`() = testBlocking {
        val itemsSize = 5
        val virtualProductId = 1L
        val sampleProductId = 2L
        val orderItems = OrderTestUtils.generateTestOrderItems(count = itemsSize)
        val refunds = emptyList<Refund>()
        val order = OrderTestUtils.generateTestOrder().copy(items = orderItems)

        whenever(orderDetailRepository.getOrderRefunds(eq(order.id))) doReturn refunds
        whenever(productDetailRepository.getProduct(any())).thenAnswer { invocation ->
            val id = invocation.arguments[0] as Long
            val isVirtual = id == virtualProductId
            val isSample = id == sampleProductId
            ProductTestUtils.generateProduct(
                productId = id,
                productName = "Product $id",
                isVirtual = isVirtual
            ).copy(isSampleProduct = isSample)
        }

        val result = sut.invoke(order)
        val items = result.first().items

        assertTrue(items.isNotEmpty())
        // result without a virtual product and a sample product should be total - 2
        assertNotEquals(items.size, itemsSize)
        assertEquals(items.size, itemsSize - 2)
        val expectedFilteredProducts = items.filter {
            it.productId == virtualProductId || it.productId == sampleProductId
        }
        assertTrue(expectedFilteredProducts.isEmpty())
    }

    @Test
    fun `when there are multiple shipments, then should return shipments with correct quantities`() = testBlocking {
        val itemsSize = 1
        val quantity = 10f
        val refunds = emptyList<Refund>()
        val item = OrderTestUtils.generateTestOrderItems(count = itemsSize, quantity = quantity).first()

        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(item))
        whenever(orderDetailRepository.getOrderRefunds(eq(order.id))) doReturn refunds
        whenever(productDetailRepository.getProduct(any())).thenAnswer { invocation ->
            val productId = invocation.arguments[0] as Long
            ProductTestUtils.generateProduct(productId = productId, productName = "Product $productId")
        }

        // When total order quantity is 10 and each shipment has 5 items
        val shipments = mapOf(
            "0" to listOf(
                ShipmentItem(
                    id = item.itemId,
                    subItems = listOf(
                        "${item.itemId}-sub-0",
                        "${item.itemId}-sub-1",
                        "${item.itemId}-sub-2",
                        "${item.itemId}-sub-3",
                        "${item.itemId}-sub-4"
                    )
                )
            ),
            "1" to listOf(
                ShipmentItem(
                    id = item.itemId,
                    subItems = listOf(
                        "${item.itemId}-sub-0",
                        "${item.itemId}-sub-1",
                        "${item.itemId}-sub-2",
                        "${item.itemId}-sub-3",
                        "${item.itemId}-sub-4"
                    )
                )
            )
        )
        whenever(wooShippingLabelRepository.getShipments(eq(order.id))) doReturn shipments

        val result = sut.invoke(order)
        val shipment1 = result.first()
        val shipment2 = result[1]

        assertEquals(shipment1.items.first().quantity, 5f)
        assertEquals(shipment2.items.first().quantity, 5f)
    }

    @Test
    fun `when label is refunded then purchased should be false`() = testBlocking {
        val orderItem = OrderTestUtils.generateTestOrderItems(count = 1).first()
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val shipmentId = "0"
        val labelId = 123L

        whenever(orderDetailRepository.getOrderRefunds(eq(order.id))) doReturn emptyList()
        whenever(productDetailRepository.getProduct(any())).thenAnswer { invocation ->
            val productId = invocation.arguments[0] as Long
            ProductTestUtils.generateProduct(productId = productId, productName = "Product $productId")
        }

        val shipments = mapOf(shipmentId to listOf(ShipmentItem(id = orderItem.itemId, subItems = emptyList())))
        val shippingLabel = ShippingLabelModel(
            labelId = labelId,
            tracking = "",
            refundableAmount = BigDecimal.ZERO,
            status = ShippingLabelStatus.PURCHASED,
            created = null,
            carrierId = "",
            serviceName = "",
            commercialInvoiceUrl = "",
            isCommercialInvoiceSubmittedElectronically = false,
            packageName = "",
            isLetter = false,
            productNames = emptyList(),
            productIds = emptyList(),
            shipmentId = shipmentId,
            receiptItemId = 0L,
            createdDate = null,
            mainReceiptId = 0L,
            rate = BigDecimal.ZERO,
            currency = "",
            expiryDate = 0L,
            usedDate = null,
            refund = ShippingLabelModel.Refund(status = "", requestDate = Date()) // Mark as refunded
        )
        whenever(wooShippingLabelRepository.getShipments(eq(order.id))) doReturn shipments
        whenever(wooShippingLabelRepository.getLabels(eq(order.id))) doReturn listOf(shippingLabel)

        val result = sut.invoke(order)
        val shipmentUIModel = result.first()

        assertFalse(shipmentUIModel.isPurchasedOrInProgress)
    }

    @Test
    fun `when label is failed, then purchased should be false`() = testBlocking {
        val orderItem = OrderTestUtils.generateTestOrderItems(count = 1).first()
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val shipmentId = "0"
        val labelId = 123L

        whenever(orderDetailRepository.getOrderRefunds(eq(order.id))) doReturn emptyList()
        whenever(productDetailRepository.getProduct(any())).thenAnswer { invocation ->
            val productId = invocation.arguments[0] as Long
            ProductTestUtils.generateProduct(productId = productId, productName = "Product $productId")
        }

        val shipments = mapOf(shipmentId to listOf(ShipmentItem(id = orderItem.itemId, subItems = emptyList())))
        val shippingLabel = ShippingLabelModel(
            labelId = labelId,
            tracking = "",
            refundableAmount = BigDecimal.ZERO,
            status = ShippingLabelStatus.PURCHASE_ERROR,
            created = null,
            carrierId = "",
            serviceName = "",
            commercialInvoiceUrl = "",
            isCommercialInvoiceSubmittedElectronically = false,
            packageName = "",
            isLetter = false,
            productNames = emptyList(),
            productIds = emptyList(),
            shipmentId = shipmentId,
            receiptItemId = 0L,
            createdDate = null,
            mainReceiptId = 0L,
            rate = BigDecimal.ZERO,
            currency = "",
            expiryDate = 0L,
            usedDate = null,
            refund = null
        )
        whenever(wooShippingLabelRepository.getShipments(eq(order.id))) doReturn shipments
        whenever(wooShippingLabelRepository.getLabels(eq(order.id))) doReturn listOf(shippingLabel)

        val result = sut.invoke(order)
        val shipmentUIModel = result.first()

        assertFalse(shipmentUIModel.isPurchasedOrInProgress)
    }

    @Test
    fun `when there is a purchased label, then shipment should be marked as purchased`() = testBlocking {
        val orderItem = OrderTestUtils.generateTestOrderItems(count = 1).first()
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val shipmentId = "0"
        val labelId = 12L

        whenever(orderDetailRepository.getOrderRefunds(eq(order.id))) doReturn emptyList()
        whenever(productDetailRepository.getProduct(any())).thenAnswer { invocation ->
            val productId = invocation.arguments[0] as Long
            ProductTestUtils.generateProduct(productId = productId, productName = "Product $productId")
        }

        val shipments = mapOf(shipmentId to listOf(ShipmentItem(id = orderItem.itemId, subItems = emptyList())))
        val shippingLabel = ShippingLabelModel(
            labelId = labelId,
            tracking = "",
            refundableAmount = BigDecimal.ZERO,
            status = ShippingLabelStatus.PURCHASED,
            created = null,
            carrierId = "",
            serviceName = "",
            commercialInvoiceUrl = "",
            isCommercialInvoiceSubmittedElectronically = false,
            packageName = "",
            isLetter = false,
            productNames = emptyList(),
            productIds = emptyList(),
            shipmentId = shipmentId,
            receiptItemId = 0L,
            createdDate = null,
            mainReceiptId = 0L,
            rate = BigDecimal.ZERO,
            currency = "",
            expiryDate = 0L,
            usedDate = null,
            refund = null
        )
        whenever(wooShippingLabelRepository.getShipments(eq(order.id))) doReturn shipments
        whenever(wooShippingLabelRepository.getLabels(eq(order.id))) doReturn listOf(shippingLabel)

        val result = sut.invoke(order)
        val shipmentUIModel = result.first()

        assertTrue(shipmentUIModel.isPurchasedOrInProgress)
        assertNotNull(shipmentUIModel.label)
    }
}
