package com.woocommerce.android.config

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsError
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsErrorType
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao
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
    fun `given fetching success, when fetchFeatureFlags is called, then get success Result`() = testBlocking {
        val fetchResult = mapOf("key" to true)
        whenever(featureFlagStore.fetchFeatureFlags(any()))
            .thenReturn(FeatureFlagsStore.FeatureFlagsResult(fetchResult))

        val result = sut.fetchFeatureFlags()
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `given fetching failure, when fetchFeatureFlags is called, then get failure Result`() = testBlocking {
        whenever(featureFlagStore.fetchFeatureFlags(any()))
            .thenReturn(FeatureFlagsStore.FeatureFlagsResult(FeatureFlagsError(FeatureFlagsErrorType.GENERIC_ERROR)))

        val result = sut.fetchFeatureFlags()
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given existing feature flag, when isRemoteFeatureFlagEnabled is called, then return the feature flag's value`() = testBlocking {
        val key = "key"
        val featureFlag = FeatureFlagConfigDao.FeatureFlag(
            key = key,
            value = true,
            createdAt = 0L,
            modifiedAt = 0L,
            source = FeatureFlagConfigDao.FeatureFlagValueSource.REMOTE
        )

        whenever(featureFlagStore.getFeatureFlagsByKey(any())).thenReturn(listOf(featureFlag))

        sut.isRemoteFeatureFlagEnabled(key)
        val result = sut.isRemoteFeatureFlagEnabled(key)
        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `given non existing feature flag, when isRemoteFeatureFlagEnabled is called, then return false`() = testBlocking {
        val key = "key"
        whenever(featureFlagStore.getFeatureFlagsByKey(any())).thenReturn(emptyList())

        val result = sut.isRemoteFeatureFlagEnabled(key)
        assertThat(result).isEqualTo(false)
    }
}
