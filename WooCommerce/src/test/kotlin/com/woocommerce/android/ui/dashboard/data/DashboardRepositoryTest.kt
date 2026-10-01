package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.lifecycle.RetainedLifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.check
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.store.WCOrderStore

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardRepositoryTest {
    private val selectedSite: SelectedSite = mock()
    private val dashboardDataStore: DashboardDataStore = mock()
    private val observeSiteOrdersState: ObserveSiteOrdersState = mock()
    private val observeBlazeWidgetStatus: ObserveBlazeWidgetStatus = mock()
    private val observePushNotificationsWidgetStatus: ObservePushNotificationsWidgetStatus = mock()
    private val observeOnboardingWidgetStatus: ObserveOnboardingWidgetStatus = mock()
    private val observeStockWidgetStatus: ObserveStockWidgetStatus = mock()
    private val observeGoogleAdsWidgetStatus: ObserveGoogleAdsWidgetStatus = mock()
    private val observeAIAssistantWidgetStatus: ObserveAIAssistantWidgetStatus = mock()
    private val activityRetainedLifecycle: ActivityRetainedLifecycle = mock()

    private val siteOrdersSource = MutableSharedFlow<DashboardWidget.Status>()
    private val blazeSource = MutableSharedFlow<DashboardWidget.Status>()

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

    @Test
    fun `given ai assistant is already stored unselected, when inserting default ai widget, then repository does not update widgets`() =
        runTest {
            // Given
            whenever(dashboardDataStore.widgets).thenReturn(
                flowOf(
                    listOf(
                        aiAssistantDataModel(isAdded = false),
                        statsDataModel(isAdded = true)
                    )
                )
            )
            val repository = createRepository()

            // When
            repository.insertAIAssistantWidgetAtTopIfMissing()

            // Then
            verify(dashboardDataStore, never()).updateDashboard(any())
        }

    @Test
    fun `given ai assistant is already stored below stats, when inserting default ai widget, then repository does not update widgets`() =
        runTest {
            // Given
            whenever(dashboardDataStore.widgets).thenReturn(
                flowOf(
                    listOf(
                        statsDataModel(isAdded = true),
                        aiAssistantDataModel(isAdded = true),
                        ordersDataModel(isAdded = true)
                    )
                )
            )
            val repository = createRepository()

            // When
            repository.insertAIAssistantWidgetAtTopIfMissing()

            // Then
            verify(dashboardDataStore, never()).updateDashboard(any())
        }

    @Test
    fun `given ai assistant is stored and status is hidden, when observing widgets, then ai assistant is hidden`() =
        runTest {
            // Given
            whenever(dashboardDataStore.widgets).thenReturn(flowOf(listOf(aiAssistantDataModel(isAdded = true))))
            whenever(selectedSite.observe()).thenReturn(flowOf(null))
            val repository = createRepository()

            // When
            val widgets = repository.widgets.first()

            // Then
            assertThat(widgets.single().type).isEqualTo(DashboardWidget.Type.AI_ASSISTANT)
            assertThat(widgets.single().status).isEqualTo(DashboardWidget.Status.Hidden)
            assertThat(widgets.single().isVisible).isFalse()
        }

    @Test
    fun `given widgets collected three times, when every collector is cancelled, then no status source is subscribed`() =
        runTest {
            // Given
            givenStatusSources(MutableStateFlow(site(name = "Store")))
            whenever(dashboardDataStore.widgets).thenReturn(flowOf(emptyList()))
            val repository = createRepository()

            // When
            collectAndCancelWidgets(repository, times = 3)

            // Then
            assertThat(siteOrdersSource.subscriptionCount.value).isEqualTo(0)
            assertThat(blazeSource.subscriptionCount.value).isEqualTo(0)
        }

    @Test
    fun `given widgets are collected, when the same store is refreshed with new data, then statuses are kept`() =
        runTest {
            // Given
            val siteFlow = MutableStateFlow<SiteModel?>(site(name = "Old name"))
            givenStatusSources(siteFlow)
            whenever(dashboardDataStore.widgets).thenReturn(
                flowOf(listOf(statsDataModel(isAdded = true), blazeDataModel(isAdded = true)))
            )
            val repository = createRepository()
            val emissions = mutableListOf<List<DashboardWidget>>()
            val collector = launch { repository.widgets.collect { emissions.add(it) } }
            advanceUntilIdle()
            siteOrdersSource.emit(DashboardWidget.Status.Available)
            blazeSource.emit(DashboardWidget.Status.Available)
            advanceUntilIdle()
            val emissionsBeforeRefresh = emissions.size

            // When
            siteFlow.value = site(name = "New name")
            advanceUntilIdle()

            // Then
            assertThat(emissions).hasSize(emissionsBeforeRefresh)
            assertThat(emissions.last().map { it.status })
                .containsExactly(DashboardWidget.Status.Available, DashboardWidget.Status.Available)
            assertThat(siteOrdersSource.subscriptionCount.value).isEqualTo(1)
            assertThat(blazeSource.subscriptionCount.value).isEqualTo(1)
            collector.cancel()
        }

    @Test
    fun `given widgets are collected, when another store is selected, then the status sources are observed again`() =
        runTest {
            // Given
            val siteFlow = MutableStateFlow<SiteModel?>(site(name = "Store"))
            givenStatusSources(siteFlow)
            whenever(dashboardDataStore.widgets).thenReturn(flowOf(listOf(blazeDataModel(isAdded = true))))
            val repository = createRepository()
            val collector = launch { repository.widgets.collect {} }
            advanceUntilIdle()

            // When
            siteFlow.value = SiteModel().apply { id = 2 }
            advanceUntilIdle()

            // Then
            verify(observeBlazeWidgetStatus, times(2)).invoke()
            assertThat(blazeSource.subscriptionCount.value).isEqualTo(1)
            collector.cancel()
        }

    @Test
    fun `given widgets collection was cancelled, when the same store is refreshed, then order statuses are not read`() =
        runTest {
            // Given
            val siteFlow = MutableStateFlow<SiteModel?>(site(name = "Old name"))
            givenStatusSources(siteFlow)
            whenever(dashboardDataStore.widgets).thenReturn(flowOf(emptyList()))
            val orderStore: WCOrderStore = mock()
            whenever(orderStore.getOrderStatusOptionsForSite(any())).thenReturn(
                listOf(WCOrderStatusModel(statusKey = "processing", statusCount = 1))
            )
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)
            val realObserveSiteOrdersState = ObserveSiteOrdersState(
                selectedSite = selectedSite,
                orderStore = orderStore,
                coroutineDispatchers = CoroutineDispatchers(testDispatcher, testDispatcher, testDispatcher),
                dispatcher = mock<Dispatcher>()
            )
            val repository = createRepository(realObserveSiteOrdersState)
            collectAndCancelWidgets(repository, times = 3)
            clearInvocations(orderStore)

            // When
            siteFlow.value = site(name = "New name")
            advanceUntilIdle()

            // Then
            verify(orderStore, never()).getOrderStatusOptionsForSite(any())
        }

    @Test
    fun `given two collectors, when widgets are collected at the same time, then each status source is subscribed once`() =
        runTest {
            // Given
            givenStatusSources(MutableStateFlow(site(name = "Store")))
            whenever(dashboardDataStore.widgets).thenReturn(flowOf(emptyList()))
            val repository = createRepository()

            // When
            val firstCollector = launch { repository.widgets.collect {} }
            val secondCollector = launch { repository.widgets.collect {} }
            advanceUntilIdle()

            // Then
            assertThat(siteOrdersSource.subscriptionCount.value).isEqualTo(1)
            assertThat(blazeSource.subscriptionCount.value).isEqualTo(1)
            firstCollector.cancel()
            secondCollector.cancel()
        }

    @Test
    fun `given a status was received, when widgets are collected again for the same store, then the last status is emitted`() =
        runTest {
            // Given
            givenStatusSources(MutableStateFlow(site(name = "Store")))
            whenever(dashboardDataStore.widgets).thenReturn(flowOf(listOf(blazeDataModel(isAdded = true))))
            val repository = createRepository()
            val collector = launch { repository.widgets.collect {} }
            advanceUntilIdle()
            blazeSource.emit(DashboardWidget.Status.Available)
            advanceUntilIdle()
            collector.cancel()
            advanceUntilIdle()

            // When
            val widgets = repository.widgets.first()

            // Then
            assertThat(widgets.single().status).isEqualTo(DashboardWidget.Status.Available)
        }

    @Test
    fun `given widgets are collected, when the retained lifecycle is cleared, then the status sources are released`() =
        runTest {
            // Given
            givenStatusSources(MutableStateFlow(site(name = "Store")))
            whenever(dashboardDataStore.widgets).thenReturn(flowOf(emptyList()))
            val repository = createRepository()
            val collector = launch { repository.widgets.collect {} }
            advanceUntilIdle()
            val onClearedListener = argumentCaptor<RetainedLifecycle.OnClearedListener>()
            verify(activityRetainedLifecycle).addOnClearedListener(onClearedListener.capture())

            // When
            onClearedListener.firstValue.onCleared()
            advanceUntilIdle()

            // Then
            assertThat(siteOrdersSource.subscriptionCount.value).isEqualTo(0)
            assertThat(blazeSource.subscriptionCount.value).isEqualTo(0)
            collector.cancel()
        }

    private fun TestScope.createRepository(
        siteOrdersState: ObserveSiteOrdersState = observeSiteOrdersState
    ): DashboardRepository {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        return DashboardRepository(
            selectedSite,
            dashboardDataStore,
            siteOrdersState,
            observeBlazeWidgetStatus,
            observePushNotificationsWidgetStatus,
            observeOnboardingWidgetStatus,
            observeStockWidgetStatus,
            observeGoogleAdsWidgetStatus,
            observeAIAssistantWidgetStatus,
            CoroutineDispatchers(testDispatcher, testDispatcher, testDispatcher),
            activityRetainedLifecycle
        )
    }

    private fun givenStatusSources(siteFlow: MutableStateFlow<SiteModel?>) {
        whenever(selectedSite.observe()).thenReturn(siteFlow)
        whenever(observeSiteOrdersState()).thenReturn(siteOrdersSource)
        whenever(observeBlazeWidgetStatus()).thenReturn(blazeSource)
        whenever(observePushNotificationsWidgetStatus()).thenReturn(MutableSharedFlow())
        whenever(observeOnboardingWidgetStatus()).thenReturn(MutableSharedFlow())
        whenever(observeStockWidgetStatus()).thenReturn(MutableSharedFlow())
        whenever(observeGoogleAdsWidgetStatus()).thenReturn(MutableSharedFlow())
        whenever(observeAIAssistantWidgetStatus()).thenReturn(MutableSharedFlow())
    }

    private fun TestScope.collectAndCancelWidgets(repository: DashboardRepository, times: Int) {
        repeat(times) {
            val collector = launch { repository.widgets.collect {} }
            advanceUntilIdle()
            collector.cancel()
            advanceUntilIdle()
        }
    }

    private fun site(name: String) = SiteModel().apply {
        id = 1
        this.name = name
    }

    private fun widgetDataModel(type: DashboardWidget.Type, isAdded: Boolean = true): DashboardWidgetDataModel =
        DashboardWidgetDataModel.newBuilder()
            .setType(type.name)
            .setIsAdded(isAdded)
            .build()

    private fun aiAssistantDataModel(isAdded: Boolean = true) =
        widgetDataModel(DashboardWidget.Type.AI_ASSISTANT, isAdded)

    private fun statsDataModel(isAdded: Boolean = true) = widgetDataModel(DashboardWidget.Type.STATS, isAdded)

    private fun ordersDataModel(isAdded: Boolean = true) = widgetDataModel(DashboardWidget.Type.ORDERS, isAdded)

    private fun blazeDataModel(isAdded: Boolean = true) = widgetDataModel(DashboardWidget.Type.BLAZE, isAdded)
}
