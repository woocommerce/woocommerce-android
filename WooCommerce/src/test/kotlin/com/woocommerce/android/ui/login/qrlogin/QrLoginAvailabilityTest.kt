package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.FeatureFlagRepository.FeatureFlagState
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginAvailabilityTest : BaseUnitTest() {

    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val deviceFeatures: DeviceFeatures = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private val availability = QrLoginAvailability(featureFlagRepository, deviceFeatures, appPrefsWrapper)

    @Before
    fun setUp() {
        whenever(featureFlagRepository.getFlagState(FeatureFlag.QR_LOGIN)).thenReturn(flagState(remote = true))
        whenever(deviceFeatures.hasCamera()).thenReturn(true)
        whenever(appPrefsWrapper.qrLoginRolloutBucket).thenReturn(1)
    }

    @Test
    fun `given flag enabled, bucket in rollout, camera present, when isAvailable, then true`() {
        assertThat(availability.isAvailable()).isTrue()
    }

    @Test
    fun `given remote flag false, when isAvailable, then false`() {
        whenever(featureFlagRepository.getFlagState(FeatureFlag.QR_LOGIN)).thenReturn(flagState(remote = false))

        assertThat(availability.isAvailable()).isFalse()
    }

    @Test
    fun `given remote flag not loaded, when isAvailable, then false`() {
        whenever(featureFlagRepository.getFlagState(FeatureFlag.QR_LOGIN)).thenReturn(flagState(remote = null))

        assertThat(availability.isAvailable()).isFalse()
    }

    @Test
    fun `given device has no camera, when isAvailable, then false`() {
        whenever(deviceFeatures.hasCamera()).thenReturn(false)

        assertThat(availability.isAvailable()).isFalse()
    }

    @Test
    fun `given bucket outside rollout, when isAvailable, then false`() {
        whenever(appPrefsWrapper.qrLoginRolloutBucket).thenReturn(2)

        assertThat(availability.isAvailable()).isFalse()
    }

    @Test
    fun `given override enabled and remote disabled, when isAvailable, then true`() {
        whenever(featureFlagRepository.getFlagState(FeatureFlag.QR_LOGIN))
            .thenReturn(flagState(remote = false, override = true))

        assertThat(availability.isAvailable()).isTrue()
    }

    @Test
    fun `given override disabled, when isAvailable, then false even if bucket in rollout`() {
        whenever(featureFlagRepository.getFlagState(FeatureFlag.QR_LOGIN))
            .thenReturn(flagState(remote = true, override = false))

        assertThat(availability.isAvailable()).isFalse()
    }

    @Test
    fun `given no bucket assigned, when isAvailable, then bucket is generated and persisted`() {
        whenever(appPrefsWrapper.qrLoginRolloutBucket).thenReturn(null)

        availability.isAvailable()

        verify(appPrefsWrapper).qrLoginRolloutBucket = argThat<Int> { this in 1..10 }
    }

    @Test
    fun `given remote true, when isAvailableForDeepLink, then true regardless of bucket`() {
        assertThat(availability.isAvailableForDeepLink()).isTrue()

        verify(appPrefsWrapper, never()).qrLoginRolloutBucket
    }

    @Test
    fun `given remote false, when isAvailableForDeepLink, then false`() {
        whenever(featureFlagRepository.getFlagState(FeatureFlag.QR_LOGIN)).thenReturn(flagState(remote = false))

        assertThat(availability.isAvailableForDeepLink()).isFalse()
    }

    @Test
    fun `given remote not loaded, when isAvailableForDeepLink, then false`() {
        whenever(featureFlagRepository.getFlagState(FeatureFlag.QR_LOGIN)).thenReturn(flagState(remote = null))

        assertThat(availability.isAvailableForDeepLink()).isFalse()
    }

    @Test
    fun `given override disabled, when isAvailableForDeepLink, then false`() {
        whenever(featureFlagRepository.getFlagState(FeatureFlag.QR_LOGIN))
            .thenReturn(flagState(remote = true, override = false))

        assertThat(availability.isAvailableForDeepLink()).isFalse()
    }

    @Test
    fun `given no camera, when isAvailableForDeepLink, then false`() {
        whenever(deviceFeatures.hasCamera()).thenReturn(false)

        assertThat(availability.isAvailableForDeepLink()).isFalse()
    }

    private fun flagState(remote: Boolean?, override: Boolean? = null) = FeatureFlagState(
        flag = FeatureFlag.QR_LOGIN,
        localValue = FeatureFlag.QR_LOGIN.localValue,
        remoteValue = remote,
        overrideValue = override
    )
}
