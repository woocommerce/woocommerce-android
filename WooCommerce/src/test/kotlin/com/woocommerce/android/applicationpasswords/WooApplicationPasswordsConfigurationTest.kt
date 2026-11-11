package com.woocommerce.android.applicationpasswords

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooApplicationPasswordsConfigurationTest {
    private val appPrefs: AppPrefsWrapper = mock {
        on { jetpackAppPasswordsEnabled } doReturn true
    }
    private val featureFlagRepository: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.APP_PASSWORDS_FOR_JETPACK_SITES) } doReturn true
    }
    private val buildConfigWrapper: BuildConfigWrapper = mock()

    private val configuration by lazy {
        WooApplicationPasswordsConfiguration(appPrefs, featureFlagRepository, buildConfigWrapper)
    }

    @Test
    fun `given a support session, when checking Jetpack access, then application passwords are disabled`() = runTest {
        // GIVEN
        whenever(buildConfigWrapper.isSupportSession).thenReturn(true)

        // WHEN
        val isEnabled = configuration.isEnabledForJetpackAccess()

        // THEN
        assertThat(isEnabled).isFalse()
    }

    @Test
    fun `given no support session, when checking Jetpack access, then application passwords are enabled`() = runTest {
        // GIVEN
        whenever(buildConfigWrapper.isSupportSession).thenReturn(false)

        // WHEN
        val isEnabled = configuration.isEnabledForJetpackAccess()

        // THEN
        assertThat(isEnabled).isTrue()
    }

    @Test
    fun `given device name contains path separators, when sanitized, then separators are replaced`() {
        // GIVEN
        val deviceName = "FIH Sharp Aquos S2 4/64\\test"

        // WHEN
        val sanitizedDeviceName = deviceName.toApplicationPasswordSafeDeviceName()

        // THEN
        assertThat(sanitizedDeviceName).isEqualTo("FIH-Sharp-Aquos-S2-4-64-test")
    }

    @Test
    fun `given device name contains spaces, when sanitized, then existing format is preserved`() {
        // GIVEN
        val deviceName = "Google Pixel 8"

        // WHEN
        val sanitizedDeviceName = deviceName.toApplicationPasswordSafeDeviceName()

        // THEN
        assertThat(sanitizedDeviceName).isEqualTo("Google-Pixel-8")
    }
}
