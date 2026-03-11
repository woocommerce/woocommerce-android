package org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork

class WooPosProductRestClientTest {
    private val wooNetwork: WooNetwork = mock()
    private val sut = WooPosProductRestClient(wooNetwork)

    @Test
    fun `given site is in -6 timezone, when adjusting timestamp, then time is shifted backwards`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T06:30:00", "-6")

        // THEN
        assertThat(result).isEqualTo("2024-01-15T00:30:00")
    }

    @Test
    fun `given site is in +5,5 timezone, when adjusting timestamp, then time is shifted forward`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T06:30:00", "5.5")

        // THEN
        assertThat(result).isEqualTo("2024-01-15T12:00:00")
    }

    @Test
    fun `given zero UTC offset, when adjusting timestamp, then time is unchanged`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T06:30:00", "0")

        // THEN
        assertThat(result).isEqualTo("2024-01-15T06:30:00")
    }

    @Test
    fun `given null offset, when adjusting timestamp, then time is unchanged`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T06:30:00", null)

        // THEN
        assertThat(result).isEqualTo("2024-01-15T06:30:00")
    }

    @Test
    fun `given non-numeric offset, when adjusting timestamp, then time is unchanged`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T06:30:00", "America/Chicago")

        // THEN
        assertThat(result).isEqualTo("2024-01-15T06:30:00")
    }

    @Test
    fun `given negative offset causing date rollback, when adjusting timestamp, then date changes correctly`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T02:00:00", "-6")

        // THEN
        assertThat(result).isEqualTo("2024-01-14T20:00:00")
    }

    @Test
    fun `given positive offset causing date rollforward, when adjusting timestamp, then date changes correctly`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T22:00:00", "5.5")

        // THEN
        assertThat(result).isEqualTo("2024-01-16T03:30:00")
    }
}
