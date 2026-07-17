package com.woocommerce.android.ui.products

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ProductStockChangedSignalTest {
    private val signal = ProductStockChangedSignal()

    @Test
    fun `when notifyStockChanged, then ids accumulate deduped in pending`() {
        signal.notifyStockChanged(listOf(1L, 2L))
        signal.notifyStockChanged(listOf(2L, 3L))

        assertThat(signal.pendingProductIds.value).containsExactlyInAnyOrder(1L, 2L, 3L)
    }

    @Test
    fun `when notifyStockChanged with empty list, then nothing is added`() {
        signal.notifyStockChanged(emptyList())

        assertThat(signal.pendingProductIds.value).isEmpty()
    }

    @Test
    fun `given pending ids, when clearProcessed, then only those are removed`() {
        signal.notifyStockChanged(listOf(1L, 2L, 3L))

        signal.clearProcessed(setOf(1L, 3L))

        assertThat(signal.pendingProductIds.value).containsExactly(2L)
    }
}
