package com.woocommerce.android.notifications.push

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NewOrderNotificationSuppressionCacheTest {
    private val cache = NewOrderNotificationSuppressionCache()

    @Test
    fun `given order created with a notifiable status, when consumed, then the entry is dropped`() {
        // GIVEN
        cache.recordOrderCreated(SITE_ID, ORDER_ID, "processing")

        // THEN
        assertThat(cache.consume(SITE_ID, ORDER_ID)).isTrue()
        assertThat(cache.consume(SITE_ID, ORDER_ID)).isFalse()
    }

    @Test
    fun `given order created with a non notifiable status, when consume, then no entry is found`() {
        // GIVEN
        cache.recordOrderCreated(SITE_ID, ORDER_ID, "pending")
        cache.recordOrderCreated(SITE_ID, ORDER_ID, "auto-draft")

        // THEN
        assertThat(cache.consume(SITE_ID, ORDER_ID)).isFalse()
    }

    @Test
    fun `given order moved from a non notifiable to a notifiable status, when consumed, then the entry is dropped`() {
        // GIVEN
        cache.recordOrderStatusChanged(SITE_ID, ORDER_ID, previousStatusKey = "pending", newStatusKey = "processing")

        // THEN
        assertThat(cache.consume(SITE_ID, ORDER_ID)).isTrue()
        assertThat(cache.consume(SITE_ID, ORDER_ID)).isFalse()
    }

    @Test
    fun `given order was already in a notifiable status, when consume, then no entry is found`() {
        // GIVEN
        cache.recordOrderStatusChanged(SITE_ID, ORDER_ID, previousStatusKey = "on-hold", newStatusKey = "completed")

        // THEN
        assertThat(cache.consume(SITE_ID, ORDER_ID)).isFalse()
    }

    @Test
    fun `given order moved to a non notifiable status, when consume, then no entry is found`() {
        // GIVEN
        cache.recordOrderStatusChanged(SITE_ID, ORDER_ID, previousStatusKey = "pending", newStatusKey = "cancelled")
        cache.recordOrderStatusChanged(SITE_ID, ORDER_ID, previousStatusKey = "processing", newStatusKey = "refunded")

        // THEN
        assertThat(cache.consume(SITE_ID, ORDER_ID)).isFalse()
    }

    @Test
    fun `given an unknown previous status, when order moves to a notifiable status, then the entry is recorded`() {
        // GIVEN
        cache.recordOrderStatusChanged(SITE_ID, ORDER_ID, previousStatusKey = null, newStatusKey = "processing")

        // THEN
        assertThat(cache.consume(SITE_ID, ORDER_ID)).isTrue()
    }

    @Test
    fun `given an order in a non notifiable status, when it is paid remotely, then the entry is recorded`() {
        // GIVEN
        cache.onOrderPaidRemotely(SITE_ID, ORDER_ID, previousStatusKey = "pending")

        // THEN
        assertThat(cache.consume(SITE_ID, ORDER_ID)).isTrue()
    }

    @Test
    fun `given an order already in a notifiable status, when it is paid remotely, then nothing is recorded`() {
        // GIVEN
        cache.onOrderPaidRemotely(SITE_ID, ORDER_ID, previousStatusKey = "on-hold")

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
            cache.recordOrderStatusChanged(
                SITE_ID,
                index.toLong(),
                previousStatusKey = "pending",
                newStatusKey = status,
            )

            // THEN
            assertThat(cache.consume(SITE_ID, index.toLong()))
                .withFailMessage("Expected an entry for status %s", status)
                .isTrue()
        }
    }

    @Test
    fun `given an entry for another order, when consume, then no entry is found`() {
        // GIVEN
        cache.recordOrderStatusChanged(SITE_ID, ORDER_ID, previousStatusKey = "pending", newStatusKey = "completed")

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
