package com.woocommerce.android.ui.woopos.orders.details.refund

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosServerRefundAvailabilityCacheTest {

    private val cache = WooPosServerRefundAvailabilityCache()

    @Test
    fun `given site not probed, when isAvailable, then returns null`() {
        assertThat(cache.isAvailable(SITE_ID)).isNull()
    }

    @Test
    fun `given site marked available, when isAvailable, then returns true`() {
        // WHEN
        cache.markAvailable(SITE_ID)

        // THEN
        assertThat(cache.isAvailable(SITE_ID)).isTrue()
    }

    @Test
    fun `given site marked unavailable, when isAvailable, then returns false`() {
        // WHEN
        cache.markUnavailable(SITE_ID)

        // THEN
        assertThat(cache.isAvailable(SITE_ID)).isFalse()
    }

    @Test
    fun `given one site unavailable, when querying another site, then it is independent`() {
        // GIVEN
        cache.markUnavailable(SITE_ID)

        // THEN
        assertThat(cache.isAvailable(OTHER_SITE_ID)).isNull()
    }

    private companion object {
        private const val SITE_ID = 1
        private const val OTHER_SITE_ID = 2
    }
}
