package com.woocommerce.android.ui.upgrades

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.support.ZendeskTags
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.plans.repository.SitePlanRepository
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesEvent
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.CurrentPlanInfo.NonUpgradeable
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.CurrentPlanInfo.Upgradeable
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
internal class UpgradesViewModelTest : BaseUnitTest() {
    lateinit var sut: UpgradesViewModel
    lateinit var selectedSite: SelectedSite

    @Before
    fun setup() {
        val siteModel = mock<SiteModel> {
            on { planId } doReturn SitePlanRepository.FREE_TRIAL_PLAN_ID
            on { planShortName } doReturn "Free Trial site"
        }
        createSut(siteModel)
    }

    @Test
    fun `when selectedSite is in free trial, then currentPlan is set to Upgradeable`() =
        testBlocking {
            // Given
            var viewModelState: UpgradesViewModel.UpgradesViewState? = null
            sut.upgradesState.observeForever {
                viewModelState = it
            }

            // Then
            assertThat(viewModelState).isNotNull
            assertThat(viewModelState?.currentPlan).isEqualTo(Upgradeable(name = "Free Trial site"))
        }

    @Test
    fun `when selectedSite is NOT in free trial, then currentPlan is set to NonUpgradeable`() =
        testBlocking {
            // Given
            val siteModel = mock<SiteModel> {
                on { planId } doReturn 0
                on { planShortName } doReturn "Not in Free Trial site"
            }
            createSut(siteModel)
            var viewModelState: UpgradesViewModel.UpgradesViewState? = null
            sut.upgradesState.observeForever {
                viewModelState = it
            }

            // Then
            assertThat(viewModelState).isNotNull
            assertThat(viewModelState?.currentPlan).isEqualTo(NonUpgradeable(name = "Not in Free Trial site"))
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
            val siteModel = mock<SiteModel> {
                on { planId } doReturn 0
            }
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

    private fun createSut(siteModel: SiteModel) {
        selectedSite = mock {
            on { observe() } doReturn flowOf(siteModel)
            on { getIfExists() } doReturn siteModel
        }

        sut = UpgradesViewModel(
            savedState = SavedStateHandle(),
            selectedSite = selectedSite
        )
    }
}
