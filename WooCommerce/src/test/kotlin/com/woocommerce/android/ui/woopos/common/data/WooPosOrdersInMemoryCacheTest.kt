package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersInMemoryCache
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class WooPosOrdersInMemoryCacheTest {

    private lateinit var cache: WooPosOrdersInMemoryCache

    @Before
    fun setup() {
        cache = WooPosOrdersInMemoryCache()
    }

    @Test
    fun `when cache is empty, then getAll returns empty list`() {
        // WHEN
        val result = cache.getAll()

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `when setAll is called, then getAll returns the same elements`() {
        // GIVEN
        val orders = listOf(
            OrderTestUtils.generateTestOrder(1),
            OrderTestUtils.generateTestOrder(2)
        )

        // WHEN
        cache.setAll(orders)
        val result = cache.getAll()

        // THEN
        assertThat(result).containsExactlyElementsOf(orders)
    }

    @Test
    fun `when setAll is called twice, then last write wins`() {
        // GIVEN
        val first = listOf(
            OrderTestUtils.generateTestOrder(1),
            OrderTestUtils.generateTestOrder(2)
        )
        val second = listOf(
            OrderTestUtils.generateTestOrder(3),
            OrderTestUtils.generateTestOrder(4),
            OrderTestUtils.generateTestOrder(5)
        )

        // WHEN
        cache.setAll(first)
        cache.setAll(second)
        val result = cache.getAll()

        // THEN
        assertThat(result).containsExactlyElementsOf(second)
    }

    @Test
    fun `when appendAll is called, then new orders are added to existing cache`() {
        // GIVEN
        val first = listOf(
            OrderTestUtils.generateTestOrder(1),
            OrderTestUtils.generateTestOrder(2)
        )
        val more = listOf(
            OrderTestUtils.generateTestOrder(3),
            OrderTestUtils.generateTestOrder(4)
        )
        cache.setAll(first)

        // WHEN
        cache.appendAll(more)

        // THEN
        assertThat(cache.getAll()).containsExactlyElementsOf(first + more)
    }

    @Test
    fun `given empty cache, when appendAll is called, then orders become the cache`() {
        // GIVEN
        val orders = listOf(
            OrderTestUtils.generateTestOrder(1),
            OrderTestUtils.generateTestOrder(2)
        )

        // WHEN
        cache.appendAll(orders)

        // THEN
        assertThat(cache.getAll()).containsExactlyElementsOf(orders)
    }

    @Test
    fun `when setAll is called after appendAll, then cache is replaced`() {
        // GIVEN
        cache.setAll(listOf(OrderTestUtils.generateTestOrder(1)))
        cache.appendAll(listOf(OrderTestUtils.generateTestOrder(2)))

        // WHEN
        val replacement = listOf(
            OrderTestUtils.generateTestOrder(10),
            OrderTestUtils.generateTestOrder(11)
        )
        cache.setAll(replacement)

        // THEN
        assertThat(cache.getAll()).containsExactlyElementsOf(replacement)
    }

    @Test
    fun `when cache is cleared, then getAll returns empty list`() {
        // GIVEN
        val orders = listOf(
            OrderTestUtils.generateTestOrder(1),
            OrderTestUtils.generateTestOrder(2)
        )
        cache.setAll(orders)

        // WHEN
        cache.clear()
        val result = cache.getAll()

        // THEN
        assertThat(result).isEmpty()
    }
}
