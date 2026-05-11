package com.woocommerce.android.ui.aiassistant

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

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
        whenever(featureFlagRepository.isEnabled(FeatureFlag.AI_ASSISTANT)).thenReturn(false)
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
            whenever(featureFlagRepository.isEnabled(FeatureFlag.AI_ASSISTANT)).thenReturn(true)
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
            whenever(featureFlagRepository.isEnabled(FeatureFlag.AI_ASSISTANT)).thenReturn(true)
            selectedSiteFlow.value = eligibleSite()

            // WHEN
            val isEligible = checker.observeEligibility().first()

            // THEN
            assertThat(isEligible).isTrue()
        }

    private companion object {
        fun eligibleSite() = SiteModel().apply {
            planActiveFeatures = "ai-assistant"
        }

        fun nonEligibleSite() = SiteModel().apply {
            planActiveFeatures = "orders"
            setIsWPComAtomic(false)
        }
    }
}
