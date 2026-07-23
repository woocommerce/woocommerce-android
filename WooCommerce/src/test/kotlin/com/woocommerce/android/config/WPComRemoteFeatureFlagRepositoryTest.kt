package com.woocommerce.android.config

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsError
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsErrorType
import org.wordpress.android.fluxc.store.mobile.FeatureFlagsStore

@OptIn(ExperimentalCoroutinesApi::class)
class WPComRemoteFeatureFlagRepositoryTest : BaseUnitTest() {
    private val featureFlagStore: FeatureFlagsStore = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        on { remoteFeatureFlagsDeviceId } doReturn DEVICE_ID
    }
    private lateinit var sut: WPComRemoteFeatureFlagRepository

    @Before
    fun setup() {
        sut = WPComRemoteFeatureFlagRepository(
            featureFlagStore,
            appPrefsWrapper
        )
    }

    @Test
    fun `given fetching success, when fetchAndCacheFeatureFlags is called, then get success Result`() =
        testBlocking {
            val fetchResult = mapOf("key" to true)
            whenever(featureFlagStore.fetchFeatureFlags(any(), any(), any(), any(), any()))
                .thenReturn(FeatureFlagsStore.FeatureFlagsResult(fetchResult))

            val result = sut.fetchAndCacheFeatureFlags()
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `given fetching failure, when fetchAndCacheFeatureFlags is called, then get failure Result`() =
        testBlocking {
            whenever(featureFlagStore.fetchFeatureFlags(any(), any(), any(), any(), any()))
                .thenReturn(
                    FeatureFlagsStore.FeatureFlagsResult(FeatureFlagsError(FeatureFlagsErrorType.GENERIC_ERROR))
                )

            val result = sut.fetchAndCacheFeatureFlags()
            assertThat(result.isFailure).isTrue()
        }

    @Test
    fun `given a stored device id, when fetchAndCacheFeatureFlags is called, then it is sent as the device id`() =
        testBlocking {
            whenever(featureFlagStore.fetchFeatureFlags(any(), any(), any(), any(), any()))
                .thenReturn(FeatureFlagsStore.FeatureFlagsResult(emptyMap()))

            sut.fetchAndCacheFeatureFlags()

            verify(featureFlagStore).fetchFeatureFlags(
                buildNumber = any(),
                deviceId = eq(DEVICE_ID),
                identifier = any(),
                marketingVersion = any(),
                platform = any()
            )
        }

    @Test
    fun `given no stored device id, when fetchAndCacheFeatureFlags is called, then one is generated and stored`() =
        testBlocking {
            whenever(appPrefsWrapper.remoteFeatureFlagsDeviceId).thenReturn("")
            whenever(featureFlagStore.fetchFeatureFlags(any(), any(), any(), any(), any()))
                .thenReturn(FeatureFlagsStore.FeatureFlagsResult(emptyMap()))

            sut.fetchAndCacheFeatureFlags()

            val deviceIdCaptor = argumentCaptor<String>()
            verify(appPrefsWrapper).remoteFeatureFlagsDeviceId = deviceIdCaptor.capture()
            assertThat(deviceIdCaptor.firstValue).isNotEmpty()
            verify(featureFlagStore).fetchFeatureFlags(
                buildNumber = any(),
                deviceId = eq(deviceIdCaptor.firstValue),
                identifier = any(),
                marketingVersion = any(),
                platform = any()
            )
        }

    private companion object {
        const val DEVICE_ID = "device-id"
    }
}
