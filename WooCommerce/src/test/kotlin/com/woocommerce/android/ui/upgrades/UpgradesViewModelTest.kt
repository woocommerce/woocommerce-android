package com.woocommerce.android.ui.upgrades

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.support.ZendeskTags
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.plans.domain.CalculateRemainingTrialPeriod
import com.woocommerce.android.ui.plans.domain.FREE_TRIAL_PERIOD
import com.woocommerce.android.ui.plans.domain.FREE_TRIAL_PLAN_ID
import com.woocommerce.android.ui.plans.domain.SitePlan
import com.woocommerce.android.ui.plans.repository.SitePlanRepository
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesEvent
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.TrialEnded
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.TrialInProgress
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import java.time.Period
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class UpgradesViewModelTest : BaseUnitTest() {
    lateinit var sut: UpgradesViewModel
    lateinit var selectedSite: SelectedSite
    lateinit var planRepository: SitePlanRepository
    lateinit var remainingTrialPeriodUseCase: CalculateRemainingTrialPeriod
    var resourceProvider: ResourceProvider = mock()

    @Before
    fun setup() {
        val siteModel = SiteModel().apply { planId = FREE_TRIAL_PLAN_ID }
        createSut(siteModel)
    }

    @Test
    fun `when onReportSubscriptionIssueClicked is called with a free trial site, then trigger OpenSupportRequestForm with the expected values`() =
        testBlocking {
            // Given
            val events = mutableListOf<MultiLiveEvent.Event>()
            sut.event.observeForever {
                events.add(it)
            }

            // When
            sut.onReportSubscriptionIssueClicked()

            // Then
            assertThat(events).containsExactly(
                UpgradesEvent.OpenSupportRequestForm(
                    HelpOrigin.UPGRADES,
                    listOf(ZendeskTags.freeTrialTag)
                )
            )
        }

    @Test
    fun `when onReportSubscriptionIssueClicked is called without a free trial site, then trigger OpenSupportRequestForm with the expected values`() =
        testBlocking {
            // Given
            val siteModel = SiteModel().apply { planId = 0 }
            createSut(siteModel)

            val events = mutableListOf<MultiLiveEvent.Event>()
            sut.event.observeForever {
                events.add(it)
            }

            // When
            sut.onReportSubscriptionIssueClicked()

            // Then
            assertThat(events).containsExactly(
                UpgradesEvent.OpenSupportRequestForm(
                    HelpOrigin.UPGRADES,
                    emptyList()
                )
            )
        }

    private fun createSut(
        siteModel: SiteModel = SiteModel(),
        type: SitePlan.Type = SitePlan.Type.FREE_TRIAL,
        remainingTrialPeriod: Period = Period.ofDays(10)
    ) {
        val sitePlan = SitePlan(
            name = "WordPress.com $FREE_TRIAL_TEST_SITE_NAME",
            expirationDate = ZonedDateTime.now(),
            type = type
        )

        selectedSite = mock {
            on { getIfExists() } doReturn siteModel
            on { get() } doReturn siteModel
        }

        planRepository = mock {
            onBlocking { fetchCurrentPlanDetails(siteModel) } doReturn sitePlan
        }

        remainingTrialPeriodUseCase = mock {
            onBlocking { invoke(sitePlan.expirationDate) } doReturn remainingTrialPeriod
        }

        sut = UpgradesViewModel(
            SavedStateHandle(),
            selectedSite,
            planRepository,
            remainingTrialPeriodUseCase,
            resourceProvider
        )
    }

    companion object {
        private const val FREE_TRIAL_TEST_SITE_NAME = "Free Trial site"
        private const val TRIAL_ENDED_TEST_SITE_NAME = "Trial ended test site"
    }
}
