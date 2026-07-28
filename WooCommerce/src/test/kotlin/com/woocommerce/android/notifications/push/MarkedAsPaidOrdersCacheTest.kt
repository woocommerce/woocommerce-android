package com.woocommerce.android.notifications.push

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MarkedAsPaidOrdersCacheTest {
    private val cache = MarkedAsPaidOrdersCache()

    @Test
    fun `given order moved to a notifiable status, when consumed, then the entry is dropped`() {
        // GIVEN
        cache.onOrderMovedToPaidStatus(SITE_ID, ORDER_ID, "processing")

        // THEN
        assertThat(cache.consume(SITE_ID, ORDER_ID)).isTrue()
        assertThat(cache.consume(SITE_ID, ORDER_ID)).isFalse()
    }

    @Test
    fun `given order moved to a non notifiable status, when consume, then no entry is found`() {
        // GIVEN
        cache.onOrderMovedToPaidStatus(SITE_ID, ORDER_ID, "pending")
        cache.onOrderMovedToPaidStatus(SITE_ID, ORDER_ID, "cancelled")
        cache.onOrderMovedToPaidStatus(SITE_ID, ORDER_ID, "auto-draft")

        // THEN
        assertThat(cache.consume(SITE_ID, ORDER_ID)).isFalse()
    }

    @Test
    fun `when every notifiable status is recorded, then each one produces an entry`() {
        val notifiableStatuses = listOf(
            "processing",
            "on-hold",
            "completed",
            "pre-order",
            "pre-ordered",
            "partial-payment",
        )

        notifiableStatuses.forEachIndexed { index, status ->
            // WHEN
            cache.onOrderMovedToPaidStatus(SITE_ID, index.toLong(), status)

            // THEN
            assertThat(cache.consume(SITE_ID, index.toLong()))
                .withFailMessage("Expected an entry for status %s", status)
                .isTrue()
        }
    }

    @Test
    fun `given an entry for another order, when consume, then no entry is found`() {
        // GIVEN
        cache.onOrderMovedToPaidStatus(SITE_ID, ORDER_ID, "completed")

        // THEN
        assertThat(cache.consume(SITE_ID, ORDER_ID + 1)).isFalse()
        assertThat(cache.consume(SITE_ID + 1, ORDER_ID)).isFalse()
        assertThat(cache.consume(SITE_ID, ORDER_ID)).isTrue()
    }

    private companion object {
        const val SITE_ID = 123L
        const val ORDER_ID = 456L
    }
}
