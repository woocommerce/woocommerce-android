package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
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
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class GetShippableItemsTests : BaseUnitTest() {
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val productDetailRepository: ProductDetailRepository = mock()
    private val sut = GetShippableItems(orderDetailRepository, productDetailRepository)

    @Test
    fun `when order only contains refunded products then should return empty list`() = testBlocking {
        val productId = 18L
        val quantity = 2F
        val refunds = OrderTestUtils.generateItemsRefunds(
            listOf(Pair(productId, quantity.toInt()))
        )
        val order = OrderTestUtils.generateTestOrder().copy(
            items = OrderTestUtils.generateTestOrderItems(
                productId = productId,
                quantity = quantity
            )
        )

        whenever(orderDetailRepository.getOrderRefunds(eq(order.id))) doReturn refunds

        val result = sut.invoke(order)

        assertTrue(result.isEmpty())
        verify(productDetailRepository, never()).getProductAsync(any())
    }

    @Test
    fun `when order don't contain refunded products then should return expected list`() = testBlocking {
        val itemsSize = 5
        val refunds = emptyList<Refund>()
        val order = OrderTestUtils.generateTestOrder().copy(
            items = OrderTestUtils.generateTestOrderItems(
                count = itemsSize,
            )
        )
        whenever(orderDetailRepository.getOrderRefunds(eq(order.id))) doReturn refunds
        whenever(productDetailRepository.getProductAsync(any())).thenAnswer { invocation ->
            val productId = invocation.arguments[0] as Long
            ProductTestUtils.generateProduct(productId = productId, productName = "Product $productId")
        }

        val result = sut.invoke(order)

        assertTrue(result.isNotEmpty())
        assertEquals(result.size, itemsSize)
    }

    @Test
    fun `when refunded quantity is less than product quantity then should return expected list`() = testBlocking {
        val productId = 18L
        val quantity = 2F
        val itemsSize = 1
        val refunds = OrderTestUtils.generateItemsRefunds(
            listOf(Pair(productId, quantity.toInt() - 1))
        )
        val order = OrderTestUtils.generateTestOrder().copy(
            items = OrderTestUtils.generateTestOrderItems(
                count = itemsSize,
                productId = productId,
                quantity = quantity
            )
        )

        whenever(orderDetailRepository.getOrderRefunds(eq(order.id))) doReturn refunds
        whenever(productDetailRepository.getProductAsync(any())).thenAnswer { invocation ->
            val id = invocation.arguments[0] as Long
            ProductTestUtils.generateProduct(productId = id, productName = "Product $id")
        }

        val result = sut.invoke(order)

        assertTrue(result.isNotEmpty())
        assertEquals(result.size, itemsSize)
    }

    @Test
    fun `when there are virtual or sample products then those products are filtered from the result`() = testBlocking {
        val itemsSize = 5
        val virtualProductId = 1L
        val sampleProductId = 2L
        val items = OrderTestUtils.generateTestOrderItems(count = itemsSize)
        val refunds = emptyList<Refund>()
        val order = OrderTestUtils.generateTestOrder().copy(items = items)

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

        assertTrue(result.isNotEmpty())
        // result without a virtual product and a sample product should be total - 2
        assertNotEquals(result.size, itemsSize)
        assertEquals(result.size, itemsSize - 2)
        val expectedFilteredProducts = result.filter {
            it.productId == virtualProductId || it.productId == sampleProductId
        }
        assertTrue(expectedFilteredProducts.isEmpty())
    }
}
