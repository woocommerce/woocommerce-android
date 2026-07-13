package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginAvailabilityTest : BaseUnitTest() {

    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val deviceFeatures: DeviceFeatures = mock()

    private val availability = QrLoginAvailability(featureFlagRepository, deviceFeatures)

    @Before
    fun setUp() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.QR_LOGIN)).thenReturn(true)
        whenever(deviceFeatures.hasCamera()).thenReturn(true)
    }

    @Test
    fun `given flag enabled and camera present, when isAvailable, then true`() {
        assertThat(availability.isAvailable()).isTrue()
    }

    @Test
    fun `given flag disabled, when isAvailable, then false`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.QR_LOGIN)).thenReturn(false)

        assertThat(availability.isAvailable()).isFalse()
    }

    @Test
    fun `given device has no camera, when isAvailable, then false`() {
        whenever(deviceFeatures.hasCamera()).thenReturn(false)

        assertThat(availability.isAvailable()).isFalse()
    }

    @Test
    fun `given flag enabled, when isAvailableForDeepLink, then true`() {
        assertThat(availability.isAvailableForDeepLink()).isTrue()
    }

    @Test
    fun `given flag disabled, when isAvailableForDeepLink, then false`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.QR_LOGIN)).thenReturn(false)

        assertThat(availability.isAvailableForDeepLink()).isFalse()
    }

    @Test
    fun `when isAvailableForDeepLink, then camera presence is not checked`() {
        availability.isAvailableForDeepLink()

        verify(deviceFeatures, never()).hasCamera()
    }
}
