package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosIsTapToPayAvailableTest {
    private val tapToPayAvailabilityStatus: TapToPayAvailabilityStatus = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock()

    private val sut = WooPosIsTapToPayAvailable(tapToPayAvailabilityStatus, featureFlagRepository)

    @Test
    fun `given flag off, when invoked, then returns false regardless of availability`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_TAP_TO_PAY)).thenReturn(false)
        whenever(tapToPayAvailabilityStatus.invoke()).thenReturn(TapToPayAvailabilityStatus.Result.Available)

        assertThat(sut()).isFalse()
    }

    @Test
    fun `given flag on and Available, when invoked, then returns true`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_TAP_TO_PAY)).thenReturn(true)
        whenever(tapToPayAvailabilityStatus.invoke()).thenReturn(TapToPayAvailabilityStatus.Result.Available)

        assertThat(sut()).isTrue()
    }

    @Test
    fun `given flag on but country not supported, when invoked, then returns false`() {
        val unavailable: TapToPayAvailabilityStatus = mock {
            on { invoke() } doReturn TapToPayAvailabilityStatus.Result.NotAvailable.CountryNotSupported
        }
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_TAP_TO_PAY)).thenReturn(true)

        val sutForUnavailable = WooPosIsTapToPayAvailable(unavailable, featureFlagRepository)

        assertThat(sutForUnavailable()).isFalse()
    }

    @Test
    fun `given flag on but NFC missing, when invoked, then returns false`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_TAP_TO_PAY)).thenReturn(true)
        whenever(tapToPayAvailabilityStatus.invoke())
            .thenReturn(TapToPayAvailabilityStatus.Result.NotAvailable.NfcNotAvailable)

        assertThat(sut()).isFalse()
    }

    @Test
    fun `given flag on, when isFeatureFlagEnabled invoked, then returns true`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_TAP_TO_PAY)).thenReturn(true)

        assertThat(sut.isFeatureFlagEnabled()).isTrue()
    }

    @Test
    fun `given flag off, when isFeatureFlagEnabled invoked, then returns false`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_TAP_TO_PAY)).thenReturn(false)

        assertThat(sut.isFeatureFlagEnabled()).isFalse()
    }
}
