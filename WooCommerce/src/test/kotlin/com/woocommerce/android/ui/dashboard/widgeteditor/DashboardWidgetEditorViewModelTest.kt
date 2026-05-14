package com.woocommerce.android.ui.dashboard.widgeteditor

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.dashboard.data.DashboardRepository
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DashboardWidgetEditorViewModelTest : BaseUnitTest() {
    private val dashboardRepository: DashboardRepository = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private lateinit var viewModel: DashboardWidgetEditorViewModel

    @Test
    fun `given ai assistant is selected, when user unselects it and saves, then ai assistant is persisted unselected`() =
        testBlocking {
            // GIVEN
            val aiAssistant = dashboardWidget(DashboardWidget.Type.AI_ASSISTANT, isSelected = true)
            val stats = dashboardWidget(DashboardWidget.Type.STATS, isSelected = true)
            val widgets = MutableSharedFlow<List<DashboardWidget>>(replay = 1)
            setup(widgets)
            widgets.emit(listOf(aiAssistant, stats))
            viewModel.viewState.getOrAwaitValue()

            // WHEN
            viewModel.onSelectionChange(aiAssistant, false)
            viewModel.onSaveClicked()

            // THEN
            verify(dashboardRepository).updateWidgets(
                check {
                    assertThat(it.first { widget -> widget.type == DashboardWidget.Type.AI_ASSISTANT }.isSelected)
                        .isFalse()
                }
            )
        }

    @Test
    fun `given ai assistant is first, when user reorders it below stats and saves, then new order is persisted`() =
        testBlocking {
            // GIVEN
            val aiAssistant = dashboardWidget(DashboardWidget.Type.AI_ASSISTANT, isSelected = true)
            val stats = dashboardWidget(DashboardWidget.Type.STATS, isSelected = true)
            val orders = dashboardWidget(DashboardWidget.Type.ORDERS, isSelected = true)
            val widgets = MutableSharedFlow<List<DashboardWidget>>(replay = 1)
            setup(widgets)
            widgets.emit(listOf(aiAssistant, stats, orders))
            viewModel.viewState.getOrAwaitValue()

            // WHEN
            viewModel.onOrderChange(fromIndex = 0, toIndex = 1)
            viewModel.onSaveClicked()

            // THEN
            verify(dashboardRepository).updateWidgets(
                check {
                    assertThat(it.map { widget -> widget.type }).containsExactly(
                        DashboardWidget.Type.STATS,
                        DashboardWidget.Type.AI_ASSISTANT,
                        DashboardWidget.Type.ORDERS
                    )
                }
            )
        }

    private fun setup(widgets: MutableSharedFlow<List<DashboardWidget>>) {
        whenever(dashboardRepository.widgets).thenReturn(widgets)
        viewModel = DashboardWidgetEditorViewModel(
            savedState = SavedStateHandle(),
            dashboardRepository = dashboardRepository,
            analyticsTracker = analyticsTracker
        )
    }

    private fun dashboardWidget(
        type: DashboardWidget.Type,
        isSelected: Boolean = true,
        status: DashboardWidget.Status = DashboardWidget.Status.Available
    ) = DashboardWidget(type = type, isSelected = isSelected, status = status)
}
