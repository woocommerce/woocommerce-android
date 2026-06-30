package com.woocommerce.android.ui.aiassistant

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao
import org.wordpress.android.fluxc.store.mobile.FeatureFlagsStore

@OptIn(ExperimentalCoroutinesApi::class)
class AIAssistantEligibilityCheckerTest : BaseUnitTest() {
    private val selectedSiteFlow = MutableStateFlow<SiteModel?>(eligibleSite())
    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val selectedSite: SelectedSite = mock {
        on { observe() } doReturn selectedSiteFlow
    }

    private val checker = AIAssistantEligibilityChecker(
        featureFlagRepository = featureFlagRepository,
        selectedSite = selectedSite,
    )

    @Test
    fun `given assistant flag is disabled, when observing eligibility, then site is not eligible`() = testBlocking {
        // GIVEN
        whenever(featureFlagRepository.observeIsEnabled(FeatureFlag.AI_ASSISTANT)).thenReturn(flowOf(false))
        selectedSiteFlow.value = eligibleSite()

        // WHEN
        val isEligible = checker.observeEligibility().first()

        // THEN
        assertThat(isEligible).isFalse()
    }

    @Test
    fun `given flag enabled and site is not eligible for ai, when observing eligibility, then site is not eligible`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagRepository.observeIsEnabled(FeatureFlag.AI_ASSISTANT)).thenReturn(flowOf(true))
            selectedSiteFlow.value = nonEligibleSite()

            // WHEN
            val isEligible = checker.observeEligibility().first()

            // THEN
            assertThat(isEligible).isFalse()
        }

    @Test
    fun `given flag enabled and site is eligible for ai, when observing eligibility, then site is eligible`() =
        testBlocking {
            // GIVEN
            whenever(featureFlagRepository.observeIsEnabled(FeatureFlag.AI_ASSISTANT)).thenReturn(flowOf(true))
            selectedSiteFlow.value = eligibleSite()

            // WHEN
            val isEligible = checker.observeEligibility().first()

            // THEN
            assertThat(isEligible).isTrue()
        }

    @Test
    fun `given remote assistant flag is absent and site is ai eligible, when observing eligibility, then site is eligible`() =
        testBlocking {
            // GIVEN
            val remoteFlags = MutableStateFlow<List<FeatureFlagConfigDao.FeatureFlag>>(emptyList())
            val featureFlagsStore: FeatureFlagsStore = mock {
                on { observeFeatureFlags(any()) } doReturn remoteFlags
            }
            val selectedSite: SelectedSite = mock {
                on { observe() } doReturn flowOf(eligibleSite())
            }
            val repository = FeatureFlagRepository(
                featureFlagsStore = featureFlagsStore,
                selectedSite = selectedSite,
                appCoroutineScope = CoroutineScope(coroutinesTestRule.testDispatcher),
            )
            advanceUntilIdle()

            // WHEN
            val isEligible = AIAssistantEligibilityChecker(repository, selectedSite)
                .observeEligibility()
                .first()

            // THEN
            assertThat(isEligible).isTrue()
        }

    @Test
    fun `given remote assistant flag is true and site is ai eligible, when observing eligibility, then site is eligible`() =
        testBlocking {
            // GIVEN
            val remoteFlags = MutableStateFlow(
                listOf(createRemoteFlag("woo_mobile_ai_assistant", true))
            )
            val featureFlagsStore: FeatureFlagsStore = mock {
                on { observeFeatureFlags(any()) } doReturn remoteFlags
            }
            val selectedSite: SelectedSite = mock {
                on { observe() } doReturn flowOf(eligibleSite())
            }
            val repository = FeatureFlagRepository(
                featureFlagsStore = featureFlagsStore,
                selectedSite = selectedSite,
                appCoroutineScope = CoroutineScope(coroutinesTestRule.testDispatcher),
            )
            advanceUntilIdle()

            // WHEN
            val isEligible = AIAssistantEligibilityChecker(repository, selectedSite)
                .observeEligibility()
                .first()

            // THEN
            assertThat(isEligible).isTrue()
        }

    @Test
    fun `given remote assistant flag is false and site is ai eligible, when observing eligibility, then site is not eligible`() =
        testBlocking {
            // GIVEN
            val remoteFlags = MutableStateFlow(
                listOf(createRemoteFlag("woo_mobile_ai_assistant", false))
            )
            val featureFlagsStore: FeatureFlagsStore = mock {
                on { observeFeatureFlags(any()) } doReturn remoteFlags
            }
            val selectedSite: SelectedSite = mock {
                on { observe() } doReturn flowOf(eligibleSite())
            }
            val repository = FeatureFlagRepository(
                featureFlagsStore = featureFlagsStore,
                selectedSite = selectedSite,
                appCoroutineScope = CoroutineScope(coroutinesTestRule.testDispatcher),
            )
            advanceUntilIdle()

            // WHEN
            val isEligible = AIAssistantEligibilityChecker(repository, selectedSite)
                .observeEligibility()
                .first()

            // THEN
            assertThat(isEligible).isFalse()
        }

    @Test
    fun `given remote assistant flag becomes false, when observing eligibility, then site becomes ineligible`() =
        testBlocking {
            // GIVEN
            val remoteFlags = MutableStateFlow<List<FeatureFlagConfigDao.FeatureFlag>>(emptyList())
            val featureFlagsStore: FeatureFlagsStore = mock {
                on { observeFeatureFlags(any()) } doReturn remoteFlags
            }
            val selectedSite: SelectedSite = mock {
                on { observe() } doReturn flowOf(eligibleSite())
            }
            val repository = FeatureFlagRepository(
                featureFlagsStore = featureFlagsStore,
                selectedSite = selectedSite,
                appCoroutineScope = CoroutineScope(coroutinesTestRule.testDispatcher),
            )
            val emissions = mutableListOf<Boolean>()

            // WHEN
            val collection = launch {
                AIAssistantEligibilityChecker(repository, selectedSite)
                    .observeEligibility()
                    .take(2)
                    .toList(emissions)
            }
            advanceUntilIdle()
            remoteFlags.value = listOf(createRemoteFlag("woo_mobile_ai_assistant", false))
            advanceUntilIdle()
            collection.join()

            // THEN
            assertThat(emissions).containsExactly(true, false)
        }

    @Test
    fun `given remote assistant flag changes from true to false, when observing eligibility, then site becomes ineligible`() =
        testBlocking {
            // GIVEN
            val remoteFlags = MutableStateFlow(
                listOf(createRemoteFlag("woo_mobile_ai_assistant", true))
            )
            val featureFlagsStore: FeatureFlagsStore = mock {
                on { observeFeatureFlags(any()) } doReturn remoteFlags
            }
            val selectedSite: SelectedSite = mock {
                on { observe() } doReturn flowOf(eligibleSite())
            }
            val repository = FeatureFlagRepository(
                featureFlagsStore = featureFlagsStore,
                selectedSite = selectedSite,
                appCoroutineScope = CoroutineScope(coroutinesTestRule.testDispatcher),
            )
            val emissions = mutableListOf<Boolean>()

            // WHEN
            val collection = launch {
                AIAssistantEligibilityChecker(repository, selectedSite)
                    .observeEligibility()
                    .take(2)
                    .toList(emissions)
            }
            advanceUntilIdle()
            remoteFlags.value = listOf(createRemoteFlag("woo_mobile_ai_assistant", false))
            advanceUntilIdle()
            collection.join()

            // THEN
            assertThat(emissions).containsExactly(true, false)
        }

    private fun createRemoteFlag(key: String, value: Boolean) = FeatureFlagConfigDao.FeatureFlag(
        key = key,
        localSiteId = LOCAL_SITE_ID,
        value = value,
        createdAt = 0L,
        modifiedAt = 0L,
        source = FeatureFlagConfigDao.FeatureFlagValueSource.REMOTE,
    )

    private companion object {
        val LOCAL_SITE_ID = LocalId(0)

        fun eligibleSite() = SiteModel().apply {
            id = LOCAL_SITE_ID.value
            planActiveFeatures = "ai-assistant"
        }

        fun nonEligibleSite() = SiteModel().apply {
            planActiveFeatures = "orders"
            setIsWPComAtomic(false)
        }
    }
}
