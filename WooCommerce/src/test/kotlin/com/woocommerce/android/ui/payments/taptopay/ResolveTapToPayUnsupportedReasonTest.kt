package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.util.DeviceSecurityPatchProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ResolveTapToPayUnsupportedReasonTest {
    private val deviceSecurityPatchProvider: DeviceSecurityPatchProvider = mock()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-08-05T00:00:00Z"), ZoneOffset.UTC)
    private val resolveReason = ResolveTapToPayUnsupportedReason(deviceSecurityPatchProvider, clock)

    @Test
    fun `given security patch older than 12 months, when resolving, then outdated security patch returned`() {
        // GIVEN
        whenever(deviceSecurityPatchProvider.get()).thenReturn("2023-11-05")

        // WHEN
        val result = resolveReason(STRIPE_MESSAGE)

        // THEN
        assertThat(result).isEqualTo(TapToPayUnsupportedReason.OutdatedSecurityPatch)
    }

    @Test
    fun `given security patch from the last 12 months, when resolving, then Stripe message returned`() {
        // GIVEN
        whenever(deviceSecurityPatchProvider.get()).thenReturn("2026-03-01")

        // WHEN
        val result = resolveReason(STRIPE_MESSAGE)

        // THEN
        assertThat(result).isEqualTo(TapToPayUnsupportedReason.Unspecified(STRIPE_MESSAGE))
    }

    @Test
    fun `given no security patch reported, when resolving, then Stripe message returned`() {
        // GIVEN
        whenever(deviceSecurityPatchProvider.get()).thenReturn(null)

        // WHEN
        val result = resolveReason(STRIPE_MESSAGE)

        // THEN
        assertThat(result).isEqualTo(TapToPayUnsupportedReason.Unspecified(STRIPE_MESSAGE))
    }

    @Test
    fun `given unparsable security patch, when resolving, then Stripe message returned`() {
        // GIVEN
        whenever(deviceSecurityPatchProvider.get()).thenReturn("not a date")

        // WHEN
        val result = resolveReason(STRIPE_MESSAGE)

        // THEN
        assertThat(result).isEqualTo(TapToPayUnsupportedReason.Unspecified(STRIPE_MESSAGE))
    }

    private companion object {
        const val STRIPE_MESSAGE = "Device does not use Trusted Execution Environment."
    }
}
