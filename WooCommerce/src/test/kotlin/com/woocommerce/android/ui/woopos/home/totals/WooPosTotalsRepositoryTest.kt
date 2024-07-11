package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.util.DateUtils
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertFailsWith

class WooPosTotalsRepositoryTest {
    private val orderCreateEditRepository: OrderCreateEditRepository = mock()
    private val dateUtils: DateUtils = mock()
    private val getProductById: WooPosGetProductById = mock()

    private lateinit var repository: WooPosTotalsRepository

    @Before
    fun setUp() {
        repository = WooPosTotalsRepository(
            orderCreateEditRepository,
            dateUtils,
            getProductById
        )
    }

    @Test
    fun `given empty product list, when createOrderWithProducts called, then return error`() = runTest {
        // GIVEN
        val productIds = emptyList<Long>()

        // WHEN
        val result = runCatching { repository.createOrderWithProducts(productIds) }

        // THEN
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `given product ids without duplicates, when createOrderWithProducts, then items all quantity one`() = runTest {
        // GIVEN
        val productIds = listOf(1L, 2L, 3L)

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
    }

    @Test
    fun `given product ids with duplicates, when createOrderWithProducts, then items quantity is correct`() = runTest {
        // GIVEN
        val productIds = listOf(1L, 1L, 2L, 3L, 3L, 3L)

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

    fun `given empty product id list, when createOrderWithProducts, then throw IllegalStateException`() = runTest {
        // GIVEN
        val productIds = emptyList<Long>()

        // WHEN & THEN
        assertFailsWith<IllegalStateException> {
            repository.createOrderWithProducts(productIds)
        }
    }
}
