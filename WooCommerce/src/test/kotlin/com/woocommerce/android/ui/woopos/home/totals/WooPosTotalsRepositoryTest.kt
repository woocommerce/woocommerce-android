package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.util.DateUtils
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class WooPosTotalsRepositoryTest {
    private val orderCreateEditRepository: OrderCreateEditRepository = mock()
    private val getProductById: WooPosGetProductById = mock()

    private val dateUtils: DateUtils = mock()

    private lateinit var repository: WooPosTotalsRepository

    private val product1 = ProductHelper.getDefaultNewProduct(
        productType = ProductType.SIMPLE,
        isVirtual = false
    ).copy(remoteId = 1L)

    @Test
    fun `given empty product list, when createOrderWithProducts called, then return error`() = runTest {
        // GIVEN
        repository = WooPosTotalsRepository(
            orderCreateEditRepository,
            dateUtils,
            getProductById
        )
        val productIds = emptyList<Long>()

        // WHEN
        val result = runCatching { repository.createOrderWithProducts(productIds) }

        // THEN
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `given product ids without duplicates, when createOrderWithProducts, then items all quantity one`() = runTest {
        // GIVEN
        repository = WooPosTotalsRepository(
            orderCreateEditRepository,
            dateUtils,
            getProductById
        )
        val productIds = listOf(1L, 2L, 3L)

        whenever(getProductById(1L)).thenReturn(product1)
        whenever(getProductById(2L)).thenReturn(product1)
        whenever(getProductById(3L)).thenReturn(product1)

        // WHEN
        repository.createOrderWithProducts(productIds = productIds)

        // THEN
        val orderCapture = argumentCaptor<Order>()
        verify(orderCreateEditRepository).createOrUpdateOrder(
            orderCapture.capture(),
            eq("")
        )

        assertThat(orderCapture.lastValue.items.size).isEqualTo(3)
        assertThat(orderCapture.lastValue.items.map { it.quantity }).containsOnly(1f)
        assertThat(orderCapture.lastValue.items[0].name).isEqualTo(product1.name)
    }

    @Test
    fun `given product id, when createOrderWithProducts, then item name matches original product`() = runTest {
        // GIVEN
        repository = WooPosTotalsRepository(
            orderCreateEditRepository,
            dateUtils,
            getProductById
        )
        val productIds = listOf(1L)

        whenever(getProductById(1L)).thenReturn(product1)

        // WHEN
        repository.createOrderWithProducts(productIds = productIds)

        // THEN
        val orderCapture = argumentCaptor<Order>()
        verify(orderCreateEditRepository).createOrUpdateOrder(
            orderCapture.capture(),
            eq("")
        )

        assertThat(orderCapture.lastValue.items.size).isEqualTo(1)
        assertThat(orderCapture.lastValue.items[0].name).isEqualTo(product1.name)
    }

    @Test
    fun `given product ids with duplicates, when createOrderWithProducts, then items quantity is correct`() = runTest {
        // GIVEN
        repository = WooPosTotalsRepository(
            orderCreateEditRepository,
            dateUtils,
            getProductById
        )
        val productIds = listOf(1L, 1L, 2L, 3L, 3L, 3L)

        whenever(getProductById(1L)).thenReturn(product1)
        whenever(getProductById(2L)).thenReturn(product1)
        whenever(getProductById(3L)).thenReturn(product1)

        // WHEN
        repository.createOrderWithProducts(productIds = productIds)

        // THEN
        val orderCapture = argumentCaptor<Order>()
        verify(orderCreateEditRepository).createOrUpdateOrder(
            orderCapture.capture(),
            eq("")
        )

        assertThat(orderCapture.lastValue.items.size).isEqualTo(3)
        assertThat(orderCapture.lastValue.items.map { it.quantity }).containsExactly(2f, 1f, 3f)
    }

    @Test
    fun `given product ids, when createOrder with some invalid ids, then return failure`() = runTest {
        // GIVEN
        repository = WooPosTotalsRepository(
            orderCreateEditRepository,
            dateUtils,
            getProductById
        )
        val productIds = listOf(1L, -1L, 3L)
        val mockOrder: Order = mock()
        whenever(orderCreateEditRepository.createOrUpdateOrder(any(), eq(""))).thenReturn(Result.success(mockOrder))

        // WHEN
        val result = runCatching { repository.createOrderWithProducts(productIds) }

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Invalid product ID: -1")
        verify(orderCreateEditRepository, never()).createOrUpdateOrder(any(), eq(""))
    }
}
