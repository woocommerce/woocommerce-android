package com.woocommerce.android.ui.woopos.orders.details.refund

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosServerRefundAvailabilityCacheTest {

    private val cache = WooPosServerRefundAvailabilityCache()

    @Test
    fun `given site not probed, when isAvailable, then returns null`() {
        assertThat(cache.isAvailable(SITE_ID, WOO_VERSION)).isNull()
    }

    @Test
    fun `given site marked available, when isAvailable for the same version, then returns true`() {
        // WHEN
        cache.markAvailable(SITE_ID, WOO_VERSION)

        // THEN
        assertThat(cache.isAvailable(SITE_ID, WOO_VERSION)).isTrue()
    }

    @Test
    fun `given site marked unavailable, when isAvailable for the same version, then returns false`() {
        // WHEN
        cache.markUnavailable(SITE_ID, WOO_VERSION)

        // THEN
        assertThat(cache.isAvailable(SITE_ID, WOO_VERSION)).isFalse()
    }

    @Test
    fun `given one site unavailable, when querying another site, then it is independent`() {
        // GIVEN
        cache.markUnavailable(SITE_ID, WOO_VERSION)

        // THEN
        assertThat(cache.isAvailable(OTHER_SITE_ID, WOO_VERSION)).isNull()
    }

    @Test
    fun `given unavailable on an older version, when the store upgrades, then the verdict no longer applies`() {
        // GIVEN a store probed as lacking the endpoints while it ran an older WooCommerce
        cache.markUnavailable(SITE_ID, OLDER_WOO_VERSION)

        // THEN the upgraded store is re-probed rather than kept on local calculation
        assertThat(cache.isAvailable(SITE_ID, WOO_VERSION)).isNull()
    }

    @Test
    fun `given available on a newer version, when the store downgrades, then the verdict no longer applies`() {
        // GIVEN a successful probe against a version that supports the endpoints
        cache.markAvailable(SITE_ID, WOO_VERSION)

        // THEN a downgraded store must not inherit it: a stale `true` would allow a computed
        // create against a store that silently drops `compute_totals`.
        assertThat(cache.isAvailable(SITE_ID, OLDER_WOO_VERSION)).isNull()
    }

    @Test
    fun `given a verdict, when the store is re-probed on a new version, then the latest verdict wins`() {
        // GIVEN
        cache.markUnavailable(SITE_ID, OLDER_WOO_VERSION)

        // WHEN the upgraded store probes successfully
        cache.markAvailable(SITE_ID, WOO_VERSION)

        // THEN
        assertThat(cache.isAvailable(SITE_ID, WOO_VERSION)).isTrue()
        assertThat(cache.isAvailable(SITE_ID, OLDER_WOO_VERSION)).isNull()
    }

    private companion object {
        private const val SITE_ID = 1
        private const val OTHER_SITE_ID = 2
        private const val WOO_VERSION = "11.1.0"
        private const val OLDER_WOO_VERSION = "11.0.5"
    }
}
