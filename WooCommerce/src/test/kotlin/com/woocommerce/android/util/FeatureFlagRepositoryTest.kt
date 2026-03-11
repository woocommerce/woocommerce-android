package com.woocommerce.android.util

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao
import org.wordpress.android.fluxc.store.mobile.FeatureFlagsStore

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureFlagRepositoryTest : BaseUnitTest() {
    private val remoteFlags = MutableStateFlow<List<FeatureFlagConfigDao.FeatureFlag>>(emptyList())
    private val featureFlagsStore: FeatureFlagsStore = mock {
        on { observeFeatureFlags() } doReturn remoteFlags
    }
    private lateinit var sut: FeatureFlagRepository

    @Before
    fun setup() {
        sut = FeatureFlagRepository(featureFlagsStore, CoroutineScope(coroutinesTestRule.testDispatcher))
    }

    @Test
    fun `given remote is true, when getFlagState called, then remoteValue is true`() = testBlocking {
        // GIVEN
        val flag = FeatureFlag.POS_REFUNDS
        remoteFlags.value = listOf(createRemoteFlag(flag.remoteFlagKey, true))
        advanceUntilIdle()

        // WHEN
        val state = sut.getFlagState(flag)

        // THEN
        assertThat(state.remoteValue).isTrue()
    }

    @Test
    fun `given remote is false, when getFlagState called, then remoteValue is false`() = testBlocking {
        // GIVEN
        val flag = FeatureFlag.POS_REFUNDS
        remoteFlags.value = listOf(createRemoteFlag(flag.remoteFlagKey, false))
        advanceUntilIdle()

        // WHEN
        val state = sut.getFlagState(flag)

        // THEN
        assertThat(state.remoteValue).isFalse()
    }

    @Test
    fun `given remote is null, when getFlagState called, then remoteValue is null`() = testBlocking {
        // GIVEN
        val flag = FeatureFlag.POS_REFUNDS

        // WHEN
        val state = sut.getFlagState(flag)

        // THEN
        assertThat(state.remoteValue).isNull()
    }

    @Test
    fun `given flag with default false, when getFlagState called, then defaultValue is false`() = testBlocking {
        // GIVEN
        val flag = FeatureFlag.WOO_PUSH_NOTIFICATIONS_SYSTEM

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

    private fun createRemoteFlag(key: String, value: Boolean) = FeatureFlagConfigDao.FeatureFlag(
        key = key,
        value = value,
        createdAt = 0L,
        modifiedAt = 0L,
        source = FeatureFlagConfigDao.FeatureFlagValueSource.REMOTE
    )
}
