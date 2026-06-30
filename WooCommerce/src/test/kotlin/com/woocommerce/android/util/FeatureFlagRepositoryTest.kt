package com.woocommerce.android.util

import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao
import org.wordpress.android.fluxc.store.mobile.FeatureFlagsStore

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureFlagRepositoryTest : BaseUnitTest() {
    private val remoteFlags = MutableStateFlow<List<FeatureFlagConfigDao.FeatureFlag>>(emptyList())
    private val secondSiteRemoteFlags = MutableStateFlow<List<FeatureFlagConfigDao.FeatureFlag>>(emptyList())
    private val featureFlagsStore: FeatureFlagsStore = mock {
        on { observeFeatureFlags(LOCAL_SITE_ID) } doReturn remoteFlags
        on { observeFeatureFlags(SECOND_LOCAL_SITE_ID) } doReturn secondSiteRemoteFlags
    }
    private val selectedSiteFlow = MutableStateFlow<SiteModel?>(SITE)
    private val selectedSite: SelectedSite = mock {
        on { observe() } doReturn selectedSiteFlow
    }
    private lateinit var sut: FeatureFlagRepository

    @Before
    fun setup() {
        sut = FeatureFlagRepository(
            featureFlagsStore,
            selectedSite,
            CoroutineScope(coroutinesTestRule.testDispatcher)
        )
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
    fun `given assistant feature flag, when inspected, then local value is enabled`() {
        // WHEN
        val localValue = FeatureFlag.AI_ASSISTANT.localValue

        // THEN
        assertThat(localValue).isTrue()
    }

    @Test
    fun `given ai support chat feature flag, when inspected, then local value is enabled`() {
        // WHEN
        val localValue = FeatureFlag.AI_SUPPORT_CHAT.localValue

        // THEN
        assertThat(localValue).isTrue()
    }

    @Test
    fun `given assistant feature flag, when inspected, then remote key matches release key`() {
        // WHEN
        val remoteFlagKey = FeatureFlag.AI_ASSISTANT.remoteFlagKey

        // THEN
        assertThat(remoteFlagKey).isEqualTo("woo_mobile_ai_assistant")
    }

    @Test
    fun `given assistant remote flag key is true, when getFlagState called, then remoteValue is true`() =
        testBlocking {
            // GIVEN
            remoteFlags.value = listOf(createRemoteFlag("woo_mobile_ai_assistant", true))
            advanceUntilIdle()

            // WHEN
            val state = sut.getFlagState(FeatureFlag.AI_ASSISTANT)

            // THEN
            assertThat(state.remoteValue).isTrue()
            assertThat(state.effectiveValue).isTrue()
        }

    @Test
    fun `given assistant remote flag key is false, when getFlagState called, then effective value is false`() =
        testBlocking {
            // GIVEN
            remoteFlags.value = listOf(createRemoteFlag("woo_mobile_ai_assistant", false))
            advanceUntilIdle()

            // WHEN
            val state = sut.getFlagState(FeatureFlag.AI_ASSISTANT)

            // THEN
            assertThat(state.localValue).isTrue()
            assertThat(state.remoteValue).isFalse()
            assertThat(state.effectiveValue).isFalse()
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
    fun `given selected site changes, when getFlagState called, then previous remote value is cleared`() =
        testBlocking {
            // GIVEN
            val flag = FeatureFlag.WC_SHIPPING_BANNER
            remoteFlags.value = listOf(createRemoteFlag(flag.remoteFlagKey, false))
            advanceUntilIdle()

            // WHEN
            selectedSiteFlow.value = SiteModel().apply { id = SECOND_LOCAL_SITE_ID.value }
            advanceUntilIdle()
            val state = sut.getFlagState(flag)

            // THEN
            assertThat(state.remoteValue).isNull()
            assertThat(state.effectiveValue).isTrue()
        }

    @Test
    fun `given self driven push notifications feature flag, when inspected, then local value follows debug build`() {
        // WHEN
        val localValue = FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1.localValue

        // THEN
        assertThat(localValue).isEqualTo(PackageUtils.isDebugBuild())
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
    fun `given local is false and remote is true, when effectiveValue accessed, then remote enables feature`() {
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
        assertThat(effectiveValue).isTrue()
    }

    @Test
    fun `given local is false and remote is null, when effectiveValue accessed, then feature stays disabled`() {
        // GIVEN
        val state = FeatureFlagRepository.FeatureFlagState(
            flag = FeatureFlag.WC_SHIPPING_BANNER,
            localValue = false,
            remoteValue = null,
            overrideValue = null
        )

        // WHEN
        val effectiveValue = state.effectiveValue

        // THEN
        assertThat(effectiveValue).isFalse()
    }

    @Test
    fun `given local is false and remote is false, when effectiveValue accessed, then feature stays disabled`() {
        // GIVEN
        val state = FeatureFlagRepository.FeatureFlagState(
            flag = FeatureFlag.WC_SHIPPING_BANNER,
            localValue = false,
            remoteValue = false,
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
        localSiteId = LOCAL_SITE_ID,
        value = value,
        createdAt = 0L,
        modifiedAt = 0L,
        source = FeatureFlagConfigDao.FeatureFlagValueSource.REMOTE
    )

    private companion object {
        val LOCAL_SITE_ID = LocalId(1)
        val SECOND_LOCAL_SITE_ID = LocalId(2)
        val SITE = SiteModel().apply { id = LOCAL_SITE_ID.value }
    }
}
