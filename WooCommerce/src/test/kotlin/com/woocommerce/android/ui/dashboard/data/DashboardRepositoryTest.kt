package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DashboardRepositoryTest {
    private val selectedSite: SelectedSite = mock()
    private val dashboardDataStore: DashboardDataStore = mock()
    private val observeSiteOrdersState: ObserveSiteOrdersState = mock()
    private val observeBlazeWidgetStatus: ObserveBlazeWidgetStatus = mock()
    private val observePushNotificationsWidgetStatus: ObservePushNotificationsWidgetStatus = mock()
    private val observeOnboardingWidgetStatus: ObserveOnboardingWidgetStatus = mock()
    private val observeStockWidgetStatus: ObserveStockWidgetStatus = mock()
    private val observeGoogleAdsWidgetStatus: ObserveGoogleAdsWidgetStatus = mock()
    private val observeInboxWidgetStatus: ObserveInboxWidgetStatus = mock()

    @Test
    fun `given site component is null, when repository is initialized, then it completes without crash`() = runTest {
        // Given
        whenever(dashboardDataStore.widgets).thenReturn(flowOf(emptyList()))
        whenever(selectedSite.observe()).thenReturn(flowOf(null))
        whenever(selectedSite.siteComponent).thenReturn(null)

        // When
        val repository = DashboardRepository(
            selectedSite,
            dashboardDataStore,
            observeSiteOrdersState,
            observeBlazeWidgetStatus,
            observePushNotificationsWidgetStatus,
            observeOnboardingWidgetStatus,
            observeStockWidgetStatus,
            observeGoogleAdsWidgetStatus,
            observeInboxWidgetStatus
        )

        // Then
        assertNotNull(repository)
    }
}
