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
        val flag = FeatureFlag.WC_SHIPPING_BANNER
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
        val flag = FeatureFlag.WC_SHIPPING_BANNER
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
        val flag = FeatureFlag.WC_SHIPPING_BANNER

        // WHEN
        val state = sut.getFlagState(flag)

        // THEN
        assertThat(state.remoteValue).isNull()
    }

    @Test
    fun `given flag with local false, when getFlagState called, then localValue is false`() = testBlocking {
        // GIVEN
        val flag = FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1

        // WHEN
        val state = sut.getFlagState(flag)

        // THEN
        assertThat(state.localValue).isFalse()
    }

    @Test
    fun `given override is set, when effectiveValue accessed, then override wins over local and remote`() {
        // GIVEN
        val stateWithOverride = FeatureFlagRepository.FeatureFlagState(
            flag = FeatureFlag.WC_SHIPPING_BANNER,
            localValue = false,
            remoteValue = false,
            overrideValue = true
        )

        // WHEN
        val effectiveValue = stateWithOverride.effectiveValue

        // THEN
        assertThat(effectiveValue).isTrue()
    }

    @Test
    fun `given local is false and remote is true, when effectiveValue accessed, then local keeps feature disabled`() {
        // GIVEN
        val state = FeatureFlagRepository.FeatureFlagState(
            flag = FeatureFlag.WC_SHIPPING_BANNER,
            localValue = false,
            remoteValue = true,
            overrideValue = null
        )

        // WHEN
        val effectiveValue = state.effectiveValue

        // THEN
        assertThat(effectiveValue).isFalse()
    }

    @Test
    fun `given local is true and remote is false, when effectiveValue accessed, then remote disables feature`() {
        // GIVEN
        val state = FeatureFlagRepository.FeatureFlagState(
            flag = FeatureFlag.WC_SHIPPING_BANNER,
            localValue = true,
            remoteValue = false,
            overrideValue = null
        )

        // WHEN
        val effectiveValue = state.effectiveValue

        // THEN
        assertThat(effectiveValue).isFalse()
    }

    @Test
    fun `given local is true and remote is true, when effectiveValue accessed, then feature stays enabled`() {
        // GIVEN
        val state = FeatureFlagRepository.FeatureFlagState(
            flag = FeatureFlag.WC_SHIPPING_BANNER,
            localValue = true,
            remoteValue = true,
            overrideValue = null
        )

        // WHEN
        val effectiveValue = state.effectiveValue

        // THEN
        assertThat(effectiveValue).isTrue()
    }

    @Test
    fun `given local is true and remote is null, when effectiveValue accessed, then local value is used`() {
        // GIVEN
        val state = FeatureFlagRepository.FeatureFlagState(
            flag = FeatureFlag.WC_SHIPPING_BANNER,
            localValue = true,
            remoteValue = null,
            overrideValue = null
        )

        // WHEN
        val effectiveValue = state.effectiveValue

        // THEN
        assertThat(effectiveValue).isTrue()
    }

    private fun createRemoteFlag(key: String, value: Boolean) = FeatureFlagConfigDao.FeatureFlag(
        key = key,
        value = value,
        createdAt = 0L,
        modifiedAt = 0L,
        source = FeatureFlagConfigDao.FeatureFlagValueSource.REMOTE
    )
}
