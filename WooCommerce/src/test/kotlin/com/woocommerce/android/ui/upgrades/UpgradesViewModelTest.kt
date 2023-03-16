package com.woocommerce.android.ui.upgrades

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.support.ZendeskTags
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.plans.domain.FREE_TRIAL_PLAN_ID
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesEvent
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class UpgradesViewModelTest : BaseUnitTest() {
    lateinit var sut: UpgradesViewModel
    lateinit var selectedSite: SelectedSite

    @Before
    fun setup() {
        val siteModel = SiteModel().apply {
            planId = FREE_TRIAL_PLAN_ID
        }
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

    private fun createSut(siteModel: SiteModel) {
        selectedSite = mock {
            on { getIfExists() } doReturn siteModel
        }

        sut = UpgradesViewModel(
            savedState = SavedStateHandle(),
            selectedSite = selectedSite,
            mock(),
            mock(),
            mock()
        )
    }
}
