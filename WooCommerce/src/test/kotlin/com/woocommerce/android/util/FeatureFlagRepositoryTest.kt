package com.woocommerce.android.util

import com.woocommerce.android.config.WPComRemoteFeatureFlagRepository
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FeatureFlagRepositoryTest {

    private val remoteFeatureFlagRepository: WPComRemoteFeatureFlagRepository = mock()
    private val sut = FeatureFlagRepository(remoteFeatureFlagRepository)

    @Test
    fun `given remote is true, when getFlagState called, then remoteValue is true`() = runTest {
        // GIVEN
        val flag = FeatureFlag.POS_REFUNDS
        whenever(remoteFeatureFlagRepository.isRemoteFeatureFlagEnabled(flag.remoteFlagKey)).thenReturn(true)

        // WHEN
        val state = sut.getFlagState(flag)

        // THEN
        assertThat(state.remoteValue).isTrue()
    }

    @Test
    fun `given remote is false, when getFlagState called, then remoteValue is false`() = runTest {
        // GIVEN
        val flag = FeatureFlag.POS_REFUNDS
        whenever(remoteFeatureFlagRepository.isRemoteFeatureFlagEnabled(flag.remoteFlagKey)).thenReturn(false)

        // WHEN
        val state = sut.getFlagState(flag)

        // THEN
        assertThat(state.remoteValue).isFalse()
    }

    @Test
    fun `given remote is null, when getFlagState called, then remoteValue is null`() = runTest {
        // GIVEN
        val flag = FeatureFlag.POS_REFUNDS
        whenever(remoteFeatureFlagRepository.isRemoteFeatureFlagEnabled(flag.remoteFlagKey)).thenReturn(null)

        // WHEN
        val state = sut.getFlagState(flag)

        // THEN
        assertThat(state.remoteValue).isNull()
    }

    @Test
    fun `given flag with default false, when getFlagState called, then defaultValue is false`() = runTest {
        // GIVEN
        val flag = FeatureFlag.WOO_PUSH_NOTIFICATIONS_SYSTEM // explicit default = false
        whenever(remoteFeatureFlagRepository.isRemoteFeatureFlagEnabled(flag.remoteFlagKey)).thenReturn(null)

        // WHEN
        val state = sut.getFlagState(flag)

        // THEN
        assertThat(state.defaultValue).isFalse()
    }

    @Test
    fun `given various states, when effectiveValue accessed, then prioritizes override over remote over default`() {
        // Override wins over everything
        val stateWithOverride = FeatureFlagRepository.FeatureFlagState(
            flag = FeatureFlag.POS_REFUNDS,
            defaultValue = false,
            remoteValue = false,
            overrideValue = true
        )
        assertThat(stateWithOverride.effectiveValue).isTrue()

        // Remote wins over default when no override
        val stateWithRemote = FeatureFlagRepository.FeatureFlagState(
            flag = FeatureFlag.POS_REFUNDS,
            defaultValue = false,
            remoteValue = true,
            overrideValue = null
        )
        assertThat(stateWithRemote.effectiveValue).isTrue()

        // Default used when no override and no remote
        val stateWithDefault = FeatureFlagRepository.FeatureFlagState(
            flag = FeatureFlag.WOO_PUSH_NOTIFICATIONS_SYSTEM,
            defaultValue = false,
            remoteValue = null,
            overrideValue = null
        )
        assertThat(stateWithDefault.effectiveValue).isFalse()
    }
}
