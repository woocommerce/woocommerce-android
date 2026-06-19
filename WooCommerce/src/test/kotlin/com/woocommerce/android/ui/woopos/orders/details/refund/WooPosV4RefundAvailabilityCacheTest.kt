package com.woocommerce.android.ui.woopos.orders.details.refund

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosV4RefundAvailabilityCacheTest {

    private val cache = WooPosV4RefundAvailabilityCache()

    @Test
    fun `given site not probed, when isV4Available, then returns null`() {
        assertThat(cache.isV4Available(SITE_ID)).isNull()
    }

    @Test
    fun `given site marked available, when isV4Available, then returns true`() {
        // WHEN
        cache.markV4Available(SITE_ID)

        // THEN
        assertThat(cache.isV4Available(SITE_ID)).isTrue()
    }

    @Test
    fun `given site marked unavailable, when isV4Available, then returns false`() {
        // WHEN
        cache.markV4Unavailable(SITE_ID)

        // THEN
        assertThat(cache.isV4Available(SITE_ID)).isFalse()
    }

    @Test
    fun `given one site unavailable, when querying another site, then it is independent`() {
        // GIVEN
        cache.markV4Unavailable(SITE_ID)

        // THEN
        assertThat(cache.isV4Available(OTHER_SITE_ID)).isNull()
    }

    private companion object {
        private const val SITE_ID = 1L
        private const val OTHER_SITE_ID = 2L
    }
}
