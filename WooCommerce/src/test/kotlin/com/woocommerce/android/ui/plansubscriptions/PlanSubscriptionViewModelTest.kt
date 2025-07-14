package com.woocommerce.android.ui.plansubscriptions

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.formatStyleFull
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.zendesk.ZendeskTags
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.plans.domain.CalculatePlanRemainingPeriod
import com.woocommerce.android.ui.plans.domain.FREE_TRIAL_PERIOD
import com.woocommerce.android.ui.plans.domain.FREE_TRIAL_PLAN_ID
import com.woocommerce.android.ui.plans.domain.SitePlan
import com.woocommerce.android.ui.plans.repository.SitePlanRepository
import com.woocommerce.android.ui.plansubscriptions.PlanSubscriptionViewModel.UpgradesViewState
import com.woocommerce.android.ui.plansubscriptions.PlanSubscriptionViewModel.UpgradesViewState.Error
import com.woocommerce.android.ui.plansubscriptions.PlanSubscriptionViewModel.UpgradesViewState.NonUpgradeable
import com.woocommerce.android.ui.plansubscriptions.PlanSubscriptionViewModel.UpgradesViewState.PlanEnded
import com.woocommerce.android.ui.plansubscriptions.PlanSubscriptionViewModel.UpgradesViewState.TrialEnded
import com.woocommerce.android.ui.plansubscriptions.PlanSubscriptionViewModel.UpgradesViewState.TrialInProgress
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel
import java.time.Period
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class PlanSubscriptionViewModelTest : BaseUnitTest() {
    lateinit var sut: PlanSubscriptionViewModel
    lateinit var selectedSite: SelectedSite
    lateinit var planRepository: SitePlanRepository
    lateinit var remainingTrialPeriodUseCase: CalculatePlanRemainingPeriod
    var resourceProvider: ResourceProvider = mock()
    var tracks: AnalyticsTrackerWrapper = mock()

    @Before
    fun setup() {
        val siteModel = SiteModel().apply { planId = FREE_TRIAL_PLAN_ID }
        createSut(siteModel)
    }

    @Test
    fun `when SitePlan is free trial with one day remaining, then state is set to TrialInProgress with expected daysLeftInFreeTrial value`() =
        testBlocking {
            // Given
            resourceProvider = mock {
                on { getString(R.string.free_trial_one_day_left, 1) } doReturn "1 day"
                on { getString(R.string.subscription_free_trial) } doReturn TEST_SITE_NAME
            }

            createSut(
                type = SitePlan.Type.FREE_TRIAL,
                remainingTrialPeriod = Period.ofDays(1)
            )
            var viewModelState: UpgradesViewState? = null
            sut.upgradesState.observeForever {
                viewModelState = it
            }

            // Then
            assertThat(viewModelState).isNotNull
            assertThat(viewModelState).isEqualTo(
                TrialInProgress(
                    name = TEST_SITE_NAME,
                    freeTrialDuration = FREE_TRIAL_PERIOD,
                    daysLeftInFreeTrial = "1 day"
                )
            )
        }

    @Test
    fun `when SitePlan is free trial with more than one day remaining, then state is set to TrialInProgress with expected daysLeftInFreeTrial value`() =
        testBlocking {
            // Given
            resourceProvider = mock {
                on { getString(R.string.free_trial_days_left_plural, 10) } doReturn "10 days"
                on { getString(R.string.subscription_free_trial) } doReturn TEST_SITE_NAME
            }

            createSut(
                type = SitePlan.Type.FREE_TRIAL,
                remainingTrialPeriod = Period.ofDays(10)
            )
            var viewModelState: UpgradesViewState? = null
            sut.upgradesState.observeForever {
                viewModelState = it
            }

            // Then
            assertThat(viewModelState).isNotNull
            assertThat(viewModelState).isEqualTo(
                TrialInProgress(
                    name = TEST_SITE_NAME,
                    freeTrialDuration = FREE_TRIAL_PERIOD,
                    daysLeftInFreeTrial = "10 days"
                )
            )
        }

    @Test
    fun `when SitePlan is free trial with no remaining time, then state is set to TrialEnded`() =
        testBlocking {
            // Given
            resourceProvider = mock {
                on { getString(any()) } doReturn TRIAL_ENDED_TEST_SITE_NAME
            }
            createSut(
                type = SitePlan.Type.FREE_TRIAL,
                remainingTrialPeriod = Period.ZERO
            )
            var viewModelState: UpgradesViewState? = null
            sut.upgradesState.observeForever {
                viewModelState = it
            }

            // Then
            assertThat(viewModelState).isNotNull
            assertThat(viewModelState).isEqualTo(
                TrialEnded(
                    name = TRIAL_ENDED_TEST_SITE_NAME
                )
            )
        }

    @Test
    fun `when SitePlan is NOT free trial with remaining days, then state is set to NonUpgradeable`() =
        testBlocking {
            // Given
            createSut(
                type = SitePlan.Type.OTHER,
                remainingTrialPeriod = Period.ofDays(1)
            )
            var viewModelState: UpgradesViewState? = null
            sut.upgradesState.observeForever {
                viewModelState = it
            }

            // Then
            assertThat(viewModelState).isNotNull
            assertThat(viewModelState).isEqualTo(
                NonUpgradeable(
                    name = TEST_SITE_NAME,
                    currentPlanEndDate = SITE_PLAN_EXPIRATION_DATE
                        .toLocalDate().formatStyleFull()
                )
            )
        }

    @Test
    fun `when SitePlan is NOT free trial WITHOUT remaining days, then state is set to NonUpgradeable`() =
        testBlocking {
            // Given
            resourceProvider = mock {
                on { getString(any(), eq(TEST_SITE_NAME)) } doReturn "$TEST_SITE_NAME ended"
            }

            createSut(
                type = SitePlan.Type.OTHER,
                remainingTrialPeriod = Period.ZERO
            )
            var viewModelState: UpgradesViewState? = null
            sut.upgradesState.observeForever {
                viewModelState = it
            }

            // Then
            assertThat(viewModelState).isNotNull
            assertThat(viewModelState).isEqualTo(
                PlanEnded(
                    name = "$TEST_SITE_NAME ended",
                )
            )
        }

    @Test
    fun `when SitePlan is null, then state is set to Error`() =
        testBlocking {
            // Given
            createSutWithoutSitePlan()
            var viewModelState: UpgradesViewState? = null
            sut.upgradesState.observeForever {
                viewModelState = it
            }

            // Then
            assertThat(viewModelState).isNotNull
            assertThat(viewModelState).isEqualTo(Error)
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
                PlanSubscriptionViewModel.OpenSupportRequestForm(
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
                PlanSubscriptionViewModel.OpenSupportRequestForm(
                    HelpOrigin.UPGRADES,
                    emptyList()
                )
            )
        }

    @Test
    fun `when onReportSubscriptionIssueClicked is called, then the expected track event is called`() {
        // When
        sut.onReportSubscriptionIssueClicked()

        // Then
        verify(tracks).track(
            AnalyticsEvent.UPGRADES_REPORT_SUBSCRIPTION_ISSUE_TAPPED,
            mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_UPGRADES_SCREEN)
        )
    }

    private fun createSut(
        siteModel: SiteModel = SiteModel(),
        type: SitePlan.Type = SitePlan.Type.FREE_TRIAL,
        remainingTrialPeriod: Period = Period.ofDays(10)
    ) {
        val sitePlan = SitePlan(
            name = "WordPress.com $TEST_SITE_NAME",
            expirationDate = SITE_PLAN_EXPIRATION_DATE,
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

        sut = PlanSubscriptionViewModel(
            SavedStateHandle(),
            selectedSite,
            planRepository,
            remainingTrialPeriodUseCase,
            resourceProvider,
            tracks
        )
    }

    private fun createSutWithoutSitePlan(
        siteModel: SiteModel = SiteModel()
    ) {
        selectedSite = mock {
            on { get() } doReturn siteModel
        }

        planRepository = mock {
            onBlocking { fetchCurrentPlanDetails(siteModel) } doReturn null
        }

        sut = PlanSubscriptionViewModel(
            SavedStateHandle(),
            selectedSite,
            planRepository,
            remainingTrialPeriodUseCase,
            resourceProvider,
            tracks
        )
    }

    companion object {
        private const val TEST_SITE_NAME = "Free Trial site"
        private const val TRIAL_ENDED_TEST_SITE_NAME = "Trial ended test site"
        private val SITE_PLAN_EXPIRATION_DATE = ZonedDateTime.now()
    }
}
