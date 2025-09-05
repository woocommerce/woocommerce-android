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
