package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingConfigDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ConfigDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.Item
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.LabelRefund
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ShippingLabelDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ShippingLabelDataDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.StoredDataDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingNetworkingMapper
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.DestinationAddressDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.OriginAddressDTO
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
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class GetShipmentsTests : BaseUnitTest() {
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val productDetailRepository: ProductDetailRepository = mock()
    private val configDataStore: WooShippingConfigDataStore = mock {
        doReturn(flowOf(null)).whenever(it).observeConfig(any())
    }
    private val mapper: WooShippingNetworkingMapper = mock()

    private val sut = GetShipments(orderDetailRepository, productDetailRepository, configDataStore, mapper)

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
            ConfigDTO(shipments = shipments, shippingLabelData = ShippingLabelDataDTO(null, null))
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
            shippingLabelData = ShippingLabelDataDTO(currentOrderLabels = listOf(shippingLabel), null)
        )
        whenever(configDataStore.observeConfig(eq(order.id))) doReturn flowOf(configDTO)

        val result = sut.invoke(order)
        val shipmentUIModel = result.first()

        assertFalse(shipmentUIModel.purchased)
    }

    @Suppress("LongMethod")
    @Test
    fun `when there are stored address in config, result should contain label with address`() = testBlocking {
        val orderItem = OrderTestUtils.generateTestOrderItems(count = 1).first()
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val shipmentId = "0"
        val labelId = 12L

        val shippingLabel = ShippingLabelDTO(labelId = labelId, shipmentId = shipmentId)
        val destinationAddressDTO = DestinationAddressDTO()
        val originAddressDTO = OriginAddressDTO()
        val configDTO = ConfigDTO(
            shipments = mapOf(shipmentId to listOf(Item(id = orderItem.itemId, subItems = emptyList()))),
            shippingLabelData = ShippingLabelDataDTO(
                currentOrderLabels = listOf(shippingLabel),
                storedData = StoredDataDTO(
                    selectedOrigin = mapOf("shipment_$shipmentId" to originAddressDTO),
                    selectedDestination = mapOf("shipment_$shipmentId" to destinationAddressDTO)
                )
            )
        )

        whenever(orderDetailRepository.getOrderRefunds(eq(order.id))) doReturn emptyList()
        whenever(productDetailRepository.getProductAsync(any())).thenAnswer { invocation ->
            val productId = invocation.arguments[0] as Long
            ProductTestUtils.generateProduct(productId = productId, productName = "Product $productId")
        }
        whenever(configDataStore.observeConfig(eq(order.id))) doReturn flowOf(configDTO)
        whenever(mapper.invoke(shippingLabel)) doReturn ShippingLabelModel(
            labelId = labelId,
            tracking = "",
            refundableAmount = BigDecimal.ZERO,
            status = ShippingLabelStatus.UNKNOWN,
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
            usedDate = 0L,
            refund = null,
        )
        whenever(mapper.invoke(destinationAddressDTO)) doReturn Address.EMPTY.copy(
            firstName = "Test",
            lastName = "Shipping"
        )

        val result = sut.invoke(order)
        val shipmentUIModel = result.first()

        assertNotNull(shipmentUIModel.label)
        assertNotNull(shipmentUIModel.label.destinationAddress)
    }
}
