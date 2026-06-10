package com.woocommerce.android.ui.designsystem

import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DesignSystemModeResolverTest {
    private val featureFlagRepository: FeatureFlagRepository = mock()

    @Test
    fun `given new design system flag disabled, when resolving default mode, then legacy mode is returned`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.NEW_DESIGN_SYSTEM)).thenReturn(false)

        val mode = DesignSystemModeResolver(featureFlagRepository).resolveDefaultMode()

        assertThat(mode).isEqualTo(DesignSystemMode.LEGACY)
    }

    @Test
    fun `given new design system flag enabled, when resolving default mode, then design system mode is returned`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.NEW_DESIGN_SYSTEM)).thenReturn(true)

        val mode = DesignSystemModeResolver(featureFlagRepository).resolveDefaultMode()

        assertThat(mode).isEqualTo(DesignSystemMode.DESIGN_SYSTEM)
    }
}
