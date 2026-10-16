package com.woocommerce.android.model

import com.woocommerce.android.ui.products.ProductTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ProductVariationTest {
    @Test
    fun `given different menu orders, when sorting, then variations are sorted by menu order`() {
        // GIVEN
        val variations = listOf(
            variation(variationId = 1, menuOrder = 2),
            variation(variationId = 2, menuOrder = 0),
            variation(variationId = 3, menuOrder = 1),
        )

        // WHEN
        val sorted = variations.sorted()

        // THEN
        assertThat(sorted.map { it.remoteVariationId }).containsExactly(2L, 3L, 1L)
    }

    @Test
    fun `given variations sharing menu orders, when sorting, then ties are sorted newest first`() {
        // GIVEN
        val variations = listOf(
            variation(variationId = 1, menuOrder = 1),
            variation(variationId = 2, menuOrder = 2),
            variation(variationId = 3, menuOrder = 2),
            variation(variationId = 4, menuOrder = 1),
        )

        // WHEN
        val sorted = variations.sorted()

        // THEN
        assertThat(sorted.map { it.remoteVariationId }).containsExactly(4L, 1L, 3L, 2L)
    }

    private fun variation(variationId: Long, menuOrder: Int) =
        ProductTestUtils.generateProductVariation(variationId = variationId).copy(menuOrder = menuOrder)
}
