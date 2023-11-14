package com.woocommerce.android.ui.plans.trial

import com.woocommerce.android.tools.ConnectivityObserver
import com.woocommerce.android.tools.ConnectivityObserver.Status.CONNECTED
import com.woocommerce.android.tools.ConnectivityObserver.Status.DISCONNECTED
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivityViewModel.BottomBarState
import com.woocommerce.android.ui.plans.domain.CalculatePlanRemainingPeriod
import com.woocommerce.android.ui.plans.domain.FREE_TRIAL_PLAN_ID
import com.woocommerce.android.ui.plans.domain.FreeTrialExpiryDateResult
import com.woocommerce.android.ui.plans.repository.SitePlanRepository
import com.woocommerce.android.ui.plans.trial.DetermineTrialStatusBarState.TrialStatusBarState.Hidden
import com.woocommerce.android.ui.plans.trial.DetermineTrialStatusBarState.TrialStatusBarState.Visible
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.wordpress.android.fluxc.model.SiteModel
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DetermineTrialStatusBarStateTest : BaseUnitTest() {

    private lateinit var sut: DetermineTrialStatusBarState

    private val selectedSite = mock<SelectedSite> {
        on { observe() } doReturn flowOf(SiteModel())
    }

    private val connectivityObserver = mock<ConnectivityObserver> {
        on { observe() } doReturn flowOf(CONNECTED)
    }

    private val sitePlanRepository = mock<SitePlanRepository>()
    private val calculatePlanRemainingPeriod = mock<CalculatePlanRemainingPeriod>()

    @Before
    fun setUp() {
        sut = DetermineTrialStatusBarState(
            sitePlanRepository,
            selectedSite,
            calculatePlanRemainingPeriod,
            connectivityObserver
        )
    }

    @Test
    fun `when connection is offline, don't show trial status bar`() = testBlocking {
        // given
        connectivityObserver.stub { on { observe() } doReturn flowOf(DISCONNECTED) }
        val bottomBarStateFlow = MutableStateFlow(BottomBarState.Visible)

        // when
        val result = sut.invoke(bottomBarStateFlow = bottomBarStateFlow)

        // then
        result.first().let {
            assertThat(it).isEqualTo(Hidden)
        }
    }

    @Test
    fun `when bottom bar is hidden, don't show trial status bar`() = testBlocking {
        // given
        val bottomBarStateFlow = MutableStateFlow(BottomBarState.Hidden)

        // when
        val result = sut.invoke(bottomBarStateFlow = bottomBarStateFlow)

        // then
        result.first().let {
            assertThat(it).isEqualTo(Hidden)
        }
    }

    @Test
    fun `when site is not free trial don't show trial status bar`() = testBlocking {
        // given
        val bottomBarStateFlow = MutableStateFlow(BottomBarState.Visible)
        selectedSite.stub {
            on { observe() } doReturn flowOf(SiteModel().apply { planId = 0 })
        }

        // when
        val result = sut.invoke(bottomBarStateFlow = bottomBarStateFlow)

        // then
        result.first().let {
            assertThat(it).isEqualTo(Hidden)
        }
    }

    @Test
    fun `when connection is online, bottom bar is visible and site is free trial, show trial status bar if API returns validity period`() =
        testBlocking {
            // given
            val bottomBarStateFlow = MutableStateFlow(BottomBarState.Visible)
            val site = SiteModel().apply { planId = FREE_TRIAL_PLAN_ID }
            selectedSite.stub {
                on { get() } doReturn site
                on { observe() } doReturn flowOf(site)
            }
            sitePlanRepository.stub {
                onBlocking { fetchFreeTrialExpiryDate(any()) } doReturn FreeTrialExpiryDateResult.ExpiryAt(
                    ANY_DATE
                )
            }
            val remainingPeriod = Period.ofDays(10)
            calculatePlanRemainingPeriod.stub {
                on { invoke(any()) } doReturn remainingPeriod
            }

            // when
            val result = sut.invoke(bottomBarStateFlow = bottomBarStateFlow)

            // then
            result.first().let {
                assertThat(it).isEqualTo(Visible(remainingPeriod.days))
            }
        }

    @Test
    fun `when connection is online, bottom bar is visible and site is free trial, hide trial status bar if API returns error`() =
        testBlocking {
            // given
            val bottomBarStateFlow = MutableStateFlow(BottomBarState.Visible)
            val site = SiteModel().apply { planId = FREE_TRIAL_PLAN_ID }
            selectedSite.stub {
                on { get() } doReturn site
                on { observe() } doReturn flowOf(site)
            }
            val errorMessage = ""
            sitePlanRepository.stub {
                onBlocking { fetchFreeTrialExpiryDate(any()) } doReturn FreeTrialExpiryDateResult.Error(
                    errorMessage
                )
            }

            // when
            val result = sut.invoke(bottomBarStateFlow = bottomBarStateFlow)

            // then
            result.first().let {
                assertThat(it).isEqualTo(Hidden)
            }
        }

    @Test
    fun `when connection is online, bottom bar is visible and site is free trial, hide trial status bar if API returns site not a trial`() =
        testBlocking {
            // given
            val bottomBarStateFlow = MutableStateFlow(BottomBarState.Visible)
            val site = SiteModel().apply { planId = FREE_TRIAL_PLAN_ID }
            selectedSite.stub {
                on { get() } doReturn site
                on { observe() } doReturn flowOf(site)
            }
            sitePlanRepository.stub {
                onBlocking { fetchFreeTrialExpiryDate(any()) } doReturn FreeTrialExpiryDateResult.NotTrial
            }

            // when
            val result = sut.invoke(bottomBarStateFlow = bottomBarStateFlow)

            // then
            result.first().let {
                assertThat(it).isEqualTo(Hidden)
            }
        }

    private companion object {
        val ANY_DATE: ZonedDateTime = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
    }
}
