package com.woocommerce.android.config

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsError
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsErrorType
import org.wordpress.android.fluxc.store.mobile.FeatureFlagsStore

@OptIn(ExperimentalCoroutinesApi::class)
class WPComRemoteFeatureFlagRepositoryTest : BaseUnitTest() {
    private val featureFlagStore: FeatureFlagsStore = mock()
    private lateinit var sut: WPComRemoteFeatureFlagRepository

    @Before
    fun setup() {
        sut = WPComRemoteFeatureFlagRepository(
            featureFlagStore
        )
    }

    @Test
    fun `given fetching success, when fetchAndCacheFeatureFlags is called, then get success Result`() =
        testBlocking {
            val fetchResult = mapOf("key" to true)
            whenever(featureFlagStore.fetchFeatureFlags(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(FeatureFlagsStore.FeatureFlagsResult(fetchResult))

            val result = sut.fetchAndCacheFeatureFlags()
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `given fetching failure, when fetchAndCacheFeatureFlags is called, then get failure Result`() =
        testBlocking {
            whenever(featureFlagStore.fetchFeatureFlags(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(
                    FeatureFlagsStore.FeatureFlagsResult(FeatureFlagsError(FeatureFlagsErrorType.GENERIC_ERROR))
                )

            val result = sut.fetchAndCacheFeatureFlags()
            assertThat(result.isFailure).isTrue()
        }

    @Test
    fun `when fetchAndCacheFeatureFlags is called with site and plugin versions, then forward context to store`() =
        testBlocking {
            // GIVEN
            val localSiteId = LocalId(123)
            val activePluginVersions = mapOf("woocommerce/woocommerce.php" to "10.9.2")
            whenever(featureFlagStore.fetchFeatureFlags(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(FeatureFlagsStore.FeatureFlagsResult(emptyMap()))

            // WHEN
            sut.fetchAndCacheFeatureFlags(
                appVersion = "1.0.0",
                localSiteId = localSiteId,
                activePluginVersions = activePluginVersions
            )

            // THEN
            verify(featureFlagStore).fetchFeatureFlags(
                buildNumber = eq(""),
                deviceId = eq(""),
                identifier = eq(""),
                marketingVersion = eq("1.0.0"),
                platform = eq("android"),
                localSiteId = eq(localSiteId),
                activePluginVersions = eq(activePluginVersions)
            )
        }
}
