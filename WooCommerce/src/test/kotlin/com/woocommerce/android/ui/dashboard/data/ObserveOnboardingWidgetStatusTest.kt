package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.R
import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.onboarding.ShouldShowOnboarding
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveOnboardingWidgetStatusTest : BaseUnitTest() {
    private val ciabSiteGateKeeper: CIABSiteGateKeeper = mock {
        on { isFeatureUnsupported(any()) } doReturn false
    }
    private val shouldShowOnboarding: ShouldShowOnboarding = mock()
    private val storeOnboardingRepository: StoreOnboardingRepository = mock()
    private val selectedSite: SelectedSite = mock {
        on { observe() } doReturn flowOf(SiteModel())
    }

    private val sut = ObserveOnboardingWidgetStatus(
        selectedSite,
        storeOnboardingRepository,
        shouldShowOnboarding,
        ciabSiteGateKeeper
    )

    @Test
    fun `given CIAB feature unsupported, when observing onboarding widget status, then status is Hidden`() =
        testBlocking {
            given(ciabSiteGateKeeper.isFeatureUnsupported(CIABAffectedFeature.Onboarding))
                .willReturn(true)

            val status = sut().first()

            assertThat(status).isEqualTo(DashboardWidget.Status.Hidden)
        }

    @Test
    fun `given CIAB feature supported and onboarding not completed, when observing, then status is Available`() =
        testBlocking {
            given(ciabSiteGateKeeper.isFeatureUnsupported(CIABAffectedFeature.Onboarding))
                .willReturn(false)
            given(shouldShowOnboarding.isOnboardingMarkedAsCompleted())
                .willReturn(false)
            given(storeOnboardingRepository.observeOnboardingTasks())
                .willReturn(flowOf(emptyList()))
            given(shouldShowOnboarding.showForTasks(emptyList()))
                .willReturn(true)

            val status = sut().first()

            assertThat(status).isEqualTo(DashboardWidget.Status.Available)
        }

    @Test
    fun `given CIAB feature supported and onboarding completed, when observing, then status is Unavailable`() =
        testBlocking {
            given(ciabSiteGateKeeper.isFeatureUnsupported(CIABAffectedFeature.Onboarding))
                .willReturn(false)
            given(shouldShowOnboarding.isOnboardingMarkedAsCompleted())
                .willReturn(true)
            given(storeOnboardingRepository.observeOnboardingTasks())
                .willReturn(flowOf(emptyList()))
            given(shouldShowOnboarding.showForTasks(emptyList()))
                .willReturn(false)

            val status = sut().first()

            assertThat(status).isEqualTo(
                DashboardWidget.Status.Unavailable(R.string.my_store_widget_onboarding_completed)
            )
        }
}
