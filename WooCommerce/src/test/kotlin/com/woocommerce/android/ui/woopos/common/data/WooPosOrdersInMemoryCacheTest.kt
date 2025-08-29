package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Order
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import java.math.BigDecimal
import java.util.Date

@ExperimentalCoroutinesApi
class WooPosOrdersInMemoryCacheTest {

    private lateinit var cache: WooPosOrdersInMemoryCache

    @Before
    fun setup() {
        cache = WooPosOrdersInMemoryCache()
    }

    @Test
    fun `when cache is empty, then getAll returns empty list`() = runTest {
        // WHEN
        val result = cache.getAll()

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `when orders are added, then getAll returns all orders`() = runTest {
        // GIVEN
        val orders = listOf(createTestOrder(1), createTestOrder(2))

        // WHEN
        cache.addAll(orders)
        val result = cache.getAll()

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyInAnyOrderElementsOf(orders)
    }

    @Test
    fun `when cache is cleared, then getAll returns empty list`() = runTest {
        // GIVEN
        val orders = listOf(createTestOrder(1), createTestOrder(2))
        cache.addAll(orders)

        // WHEN
        cache.clear()
        val result = cache.getAll()

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `when adding order with existing id, then it should replace old order`() = runTest {
        // GIVEN
        val order1 = createTestOrder(1, "Order 1")
        val order1Updated = createTestOrder(1, "Order 1 Updated")

        // WHEN
        cache.addAll(listOf(order1))
        cache.addAll(listOf(order1Updated))
        val result = cache.getAll().first { it.id == 1L }

        // THEN
        assertThat(result).isEqualTo(order1Updated)
        assertThat(result.customerNote).isEqualTo("Order 1 Updated")
    }

    @Test
    fun `when adding more orders than max cache size, then oldest orders should be removed`() = runTest {
        // GIVEN
        val orders = (1..10005L).map { createTestOrder(it) }

        // WHEN
        cache.addAll(orders)
        val firstOrder = cache.getAll().find { it.id == 1L }
        val lastOrder = cache.getAll().find { it.id == 10005L }

        // THEN
        assertThat(firstOrder).isNull()
        assertThat(lastOrder).isNotNull
        assertThat(cache.getAll()).hasSize(25)
    }

    @Test
    fun `when multiple threads access cache concurrently, then data remains consistent`() = runTest {
        // GIVEN
        val initialOrders = (1..100L).map { createTestOrder(it) }
        cache.addAll(initialOrders)

        // WHEN - simulate concurrent access
        val concurrentOperations = (101..200L).map { id ->
            async {
                val order = createTestOrder(id)
                cache.addAll(listOf(order))
                cache.getAll().find { it.id == id }
            }
        }

        // THEN
        val results = concurrentOperations.awaitAll()
        assertThat(results).hasSize(100)
        assertThat(results).doesNotContainNull()

        val allCachedOrders = cache.getAll()
        // Only MAX_CACHE_SIZE should remain
        assertThat(allCachedOrders).hasSize(25)

        // The most recent 25 orders should be present
        val expectedIds = (176L..200L).toList()
        assertThat(allCachedOrders.map { it.id }).containsExactlyInAnyOrderElementsOf(expectedIds)
    }

    private fun createTestOrder(
        id: Long,
        note: String = "Order $id"
    ): Order = mock {
        on { this.id }.thenReturn(id)
        on { customerNote }.thenReturn(note)
        on { dateCreated }.thenReturn(Date())
        on { total }.thenReturn(BigDecimal.TEN)
        on { currency }.thenReturn("USD")
        on { number }.thenReturn(id.toString())
    }
}
