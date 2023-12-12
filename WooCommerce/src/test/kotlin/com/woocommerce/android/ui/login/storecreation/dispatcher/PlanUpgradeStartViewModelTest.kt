package com.woocommerce.android.ui.login.storecreation.dispatcher

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.plans.domain.SitePlan
import com.woocommerce.android.ui.plans.repository.SitePlanRepository
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class PlanUpgradeStartViewModelTest : BaseUnitTest() {

    private lateinit var sut: PlanUpgradeStartViewModel

    private val sitePlanRepository: SitePlanRepository = mock()
    private val selectedSiteModel = SiteModel().apply {
        siteId = 456L
    }
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn selectedSiteModel
    }
    private val wooCommerceStore: WooCommerceStore = mock()

    private val sitePlan = SitePlan(
        "test",
        ZonedDateTime.of(2023, 10, 10, 0, 0, 0, 0, ZoneId.of("UTC")),
        SitePlan.Type.OTHER
    )

    private fun initialize() {
        sut = PlanUpgradeStartViewModel(
            savedStateHandle = PlanUpgradeStartFragmentArgs(
                source = PlanUpgradeStartFragment.PlanUpgradeStartSource.BANNER
            ).toSavedStateHandle(),
            wpComWebViewAuthenticator = mock(),
            userAgent = mock(),
            selectedSite = selectedSite,
            tracks = mock(),
            sitePlanRepository = sitePlanRepository,
            wooCommerceStore = wooCommerceStore
        )
    }

    @Test
    fun `given that selected site is not free trial, when initialize, then exit the screen`() {
        // Given
        sitePlanRepository.stub {
            onBlocking { fetchCurrentPlanDetails(selectedSiteModel) } doReturn sitePlan.copy(
                type = SitePlan.Type.OTHER
            )
        }

        // When
        initialize()

        // Then
        assertThat(sut.event.captureValues()).containsExactly(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `given that selected site is free trial, when initialize, then do not exit the screen`() {
        // Given
        sitePlanRepository.stub {
            onBlocking { fetchCurrentPlanDetails(selectedSiteModel) } doReturn sitePlan.copy(
                type = SitePlan.Type.FREE_TRIAL
            )
        }

        // When
        initialize()

        // Then
        assertThat(sut.event.captureValues()).isEmpty()
    }

    @Test
    fun `given that selected site fails getting the site's plan, when initialize, then exit the screen`() {
        // Given
        sitePlanRepository.stub {
            onBlocking { fetchCurrentPlanDetails(selectedSiteModel) } doReturn null
        }

        // When
        initialize()

        // Then
        assertThat(sut.event.captureValues()).containsExactly(MultiLiveEvent.Event.Exit)
    }
}
