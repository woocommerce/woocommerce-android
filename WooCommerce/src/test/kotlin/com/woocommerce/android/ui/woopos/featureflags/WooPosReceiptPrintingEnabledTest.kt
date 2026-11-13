package com.woocommerce.android.ui.woopos.featureflags

import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosReceiptPrintingEnabledTest {
    private val featureFlagRepository: FeatureFlagRepository = mock()

    private val sut = WooPosReceiptPrintingEnabled(featureFlagRepository)

    @Test
    fun `given flag off, when invoked, then returns false`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_RECEIPT_PRINTING)).thenReturn(false)

        assertThat(sut()).isFalse()
    }

    @Test
    fun `given flag on, when invoked, then returns true`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_RECEIPT_PRINTING)).thenReturn(true)

        assertThat(sut()).isTrue()
    }
}
