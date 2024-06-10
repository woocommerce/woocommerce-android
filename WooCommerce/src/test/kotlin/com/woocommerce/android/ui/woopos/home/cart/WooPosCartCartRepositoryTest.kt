package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.util.DateUtils
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.Date
import kotlin.test.Test

class WooPosCartCartRepositoryTest {
    private val mockedDate: Date = mock()
    private val mockedOrder: Order = mock()
    private val orderCreateEditRepository: OrderCreateEditRepository = mock {
        onBlocking { createOrUpdateOrder(any(), any<String>()) }.thenReturn(
            Result.success(mockedOrder)
        )
    }
    private val dateUtils: DateUtils = mock {
        on { getCurrentDateInSiteTimeZone() }.thenReturn(mockedDate)
    }

    val repository = WooPosCartRepository(
        orderCreateEditRepository = orderCreateEditRepository,
        dateUtils = dateUtils
    )

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

}
