package com.woocommerce.android.ui.dashboard.data

import android.content.Context
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardDataStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val selectedSite: SelectedSite = mock()
    private val crashLogging: CrashLogging = mock()
    private val context: Context = mock()

    @Test
    fun `given site is null, when widgets observed, then default widgets are emitted`() = runTest {
        // Given
        val dashboardDataStore = createDataStore(flowOf(null))

        // When
        val widgets = dashboardDataStore.widgets.first()

        // Then
        assertThat(widgets).isEqualTo(dashboardDataStore.getDefaultWidgets())
    }

    @Test
    fun `given default dashboard config, when widgets observed, then ai assistant is first and selected`() = runTest {
        // Given
        val dashboardDataStore = createDataStore(flowOf(null))

        // When
        val widgets = dashboardDataStore.widgets.first()

        // Then
        assertThat(widgets.first().type).isEqualTo(DashboardWidget.Type.AI_ASSISTANT.name)
        assertThat(widgets.first().isAdded).isTrue()
    }

    @Test
    fun `given config saved for a store, when switching to another store and back, then the config is kept`() =
        runTest {
            // Given
            val siteFlow = MutableStateFlow<SiteModel?>(site(id = 1))
            val dashboardDataStore = createDataStore(siteFlow)
            val storeOneWidgets = listOf(widgetDataModel(DashboardWidget.Type.STATS, isAdded = false))
            dashboardDataStore.updateDashboard(DashboardDataModel.newBuilder().addAllWidgets(storeOneWidgets).build())

            // When
            siteFlow.value = site(id = 2)
            val storeTwoWidgets = dashboardDataStore.widgets.first()
            siteFlow.value = site(id = 1)
            val storeOneWidgetsAfterSwitch = dashboardDataStore.widgets.first()

            // Then
            assertThat(storeTwoWidgets).isEqualTo(dashboardDataStore.getDefaultWidgets())
            assertThat(storeOneWidgetsAfterSwitch).isEqualTo(storeOneWidgets)
        }

    @Test
    fun `given widgets are collected, when the same store is refreshed with a new name, then widgets do not emit again`() =
        runTest {
            // Given
            val siteFlow = MutableStateFlow<SiteModel?>(site(id = 1, name = "Old name"))
            val dashboardDataStore = createDataStore(siteFlow)
            val emissions = mutableListOf<List<DashboardWidgetDataModel>>()
            val collector = launch { dashboardDataStore.widgets.collect { emissions.add(it) } }
            advanceUntilIdle()
            val emissionsBeforeRefresh = emissions.size

            // When
            siteFlow.value = site(id = 1, name = "New name")
            advanceUntilIdle()

            // Then
            assertThat(emissionsBeforeRefresh).isEqualTo(1)
            assertThat(emissions).hasSize(emissionsBeforeRefresh)
            collector.cancel()
        }

    private fun TestScope.createDataStore(siteFlow: Flow<SiteModel?>): DashboardDataStore {
        whenever(selectedSite.observe()).thenReturn(siteFlow)
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.filesDir).thenReturn(temporaryFolder.root)
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        return DashboardDataStore(
            context,
            selectedSite,
            crashLogging,
            backgroundScope,
            CoroutineDispatchers(testDispatcher, testDispatcher, testDispatcher)
        )
    }

    private fun site(id: Int, name: String = "Store $id") = SiteModel().apply {
        this.id = id
        this.name = name
    }

    private fun widgetDataModel(type: DashboardWidget.Type, isAdded: Boolean) =
        DashboardWidgetDataModel.newBuilder()
            .setType(type.name)
            .setIsAdded(isAdded)
            .build()
}
