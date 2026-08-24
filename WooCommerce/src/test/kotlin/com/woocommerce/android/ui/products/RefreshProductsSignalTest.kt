package com.woocommerce.android.ui.products

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RefreshProductsSignalTest {
    private val signal = RefreshProductsSignal()

    @Test
    fun `when notifyProductsChanged, then ids accumulate deduped in pending`() {
        signal.notifyProductsChanged(listOf(1L, 2L))
        signal.notifyProductsChanged(listOf(2L, 3L))

        assertThat(signal.pendingProductIds.value).containsExactlyInAnyOrder(1L, 2L, 3L)
    }

    @Test
    fun `when notifyProductsChanged with empty list, then nothing is added`() {
        signal.notifyProductsChanged(emptyList())

        assertThat(signal.pendingProductIds.value).isEmpty()
    }

    @Test
    fun `given pending ids, when clearProcessed, then only those are removed`() {
        signal.notifyProductsChanged(listOf(1L, 2L, 3L))

        signal.clearProcessed(setOf(1L, 3L))

        assertThat(signal.pendingProductIds.value).containsExactly(2L)
    }
}
