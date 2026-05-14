package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
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
    private val observeAIAssistantWidgetStatus: ObserveAIAssistantWidgetStatus = mock()

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
            observeInboxWidgetStatus,
            observeAIAssistantWidgetStatus
        )

        // Then
        assertNotNull(repository)
    }

    @Test
    fun `given ai assistant is missing, when inserting ai assistant, then it is persisted as first selected widget`() =
        runTest {
            // Given
            val storedWidgets = listOf(statsDataModel(isAdded = true), ordersDataModel(isAdded = true))
            whenever(dashboardDataStore.widgets).thenReturn(flowOf(storedWidgets))
            val repository = createRepository()

            // When
            repository.insertAIAssistantWidgetAtTopIfMissing()

            // Then
            verify(dashboardDataStore).updateDashboard(
                check {
                    assertThat(it.widgetsList.map { widget -> widget.type }).containsExactly(
                        DashboardWidget.Type.AI_ASSISTANT.name,
                        DashboardWidget.Type.STATS.name,
                        DashboardWidget.Type.ORDERS.name
                    )
                    assertThat(it.widgetsList.first().isAdded).isTrue()
                }
            )
        }

    private fun createRepository() = DashboardRepository(
        selectedSite,
        dashboardDataStore,
        observeSiteOrdersState,
        observeBlazeWidgetStatus,
        observePushNotificationsWidgetStatus,
        observeOnboardingWidgetStatus,
        observeStockWidgetStatus,
        observeGoogleAdsWidgetStatus,
        observeInboxWidgetStatus,
        observeAIAssistantWidgetStatus
    )

    private fun widgetDataModel(type: DashboardWidget.Type, isAdded: Boolean = true): DashboardWidgetDataModel =
        DashboardWidgetDataModel.newBuilder()
            .setType(type.name)
            .setIsAdded(isAdded)
            .build()

    private fun statsDataModel(isAdded: Boolean = true) = widgetDataModel(DashboardWidget.Type.STATS, isAdded)

    private fun ordersDataModel(isAdded: Boolean = true) = widgetDataModel(DashboardWidget.Type.ORDERS, isAdded)
}
