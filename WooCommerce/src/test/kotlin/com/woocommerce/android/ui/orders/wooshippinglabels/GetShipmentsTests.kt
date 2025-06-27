package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingConfigDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ConfigDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.Item
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.LabelRefund
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ShippingLabelDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ShippingLabelDataDTO
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class GetShipmentsTests : BaseUnitTest() {
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val productDetailRepository: ProductDetailRepository = mock()
    private val configDataStore: WooShippingConfigDataStore = mock {
        doReturn(flowOf(null)).whenever(it).observeConfig(any())
    }

    private val sut = GetShipments(orderDetailRepository, productDetailRepository, configDataStore, mock())

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
        verify(productDetailRepository, never()).getProductAsync(any())
    }

    @Test
    fun `when order don't contain refunded products then should return expected list`() = testBlocking {
        val itemsSize = 5
        val refunds = emptyList<Refund>()
        val order = OrderTestUtils.generateTestOrder().copy(
            items = OrderTestUtils.generateTestOrderItems(count = itemsSize)
        )
        whenever(orderDetailRepository.getOrderRefunds(eq(order.id))) doReturn refunds
        whenever(productDetailRepository.getProductAsync(any())).thenAnswer { invocation ->
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
        whenever(productDetailRepository.getProductAsync(any())).thenAnswer { invocation ->
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
        whenever(productDetailRepository.getProductAsync(any())).thenAnswer { invocation ->
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
        whenever(productDetailRepository.getProductAsync(any())).thenAnswer { invocation ->
            val productId = invocation.arguments[0] as Long
            ProductTestUtils.generateProduct(productId = productId, productName = "Product $productId")
        }

        // When total order quantity is 10 and each shipment has 5 items
        val shipments = mapOf(
            "0" to listOf(
                Item(
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
                Item(
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
        whenever(configDataStore.observeConfig(eq(order.id))) doReturn flowOf(
            ConfigDTO(shipments = shipments, shippingLabelData = ShippingLabelDataDTO(emptyList()))
        )

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
        whenever(productDetailRepository.getProductAsync(any())).thenAnswer { invocation ->
            val productId = invocation.arguments[0] as Long
            ProductTestUtils.generateProduct(productId = productId, productName = "Product $productId")
        }

        val shipments = mapOf(shipmentId to listOf(Item(id = orderItem.itemId, subItems = emptyList())))
        val shippingLabel = ShippingLabelDTO(
            labelId = labelId,
            shipmentId = shipmentId,
            refund = LabelRefund(status = "") // Mark as refunded
        )
        val configDTO = ConfigDTO(
            shipments = shipments,
            shippingLabelData = ShippingLabelDataDTO(currentOrderLabels = listOf(shippingLabel))
        )
        whenever(configDataStore.observeConfig(eq(order.id))) doReturn flowOf(configDTO)

        val result = sut.invoke(order)
        val shipmentUIModel = result.first()

        assertFalse(shipmentUIModel.purchased)
    }
}
