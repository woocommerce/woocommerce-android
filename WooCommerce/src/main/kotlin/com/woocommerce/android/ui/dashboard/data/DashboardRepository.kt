package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.R
import com.woocommerce.android.di.SiteComponentEntryPoint
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.toDataModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel
import dagger.hilt.EntryPoints
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@ActivityRetainedScoped
class DashboardRepository @Inject constructor(
    selectedSite: SelectedSite,
    private val dashboardDataStore: DashboardDataStore,
    observeSiteOrdersState: ObserveSiteOrdersState,
    observeBlazeWidgetStatus: ObserveBlazeWidgetStatus,
    observeOnboardingWidgetStatus: ObserveOnboardingWidgetStatus
) {
    private val siteCoroutineScope = EntryPoints.get(
        selectedSite.siteComponent!!,
        SiteComponentEntryPoint::class.java
    ).siteCoroutineScope()

    private val siteOrdersState = observeSiteOrdersState()
        .stateIn(
            scope = siteCoroutineScope,
            started = SharingStarted.Lazily,
            initialValue = DashboardWidget.Status.Unavailable(R.string.my_store_widget_unavailable)
        )

    private val blazeWidgetStatus = observeBlazeWidgetStatus()
        .stateIn(
            scope = siteCoroutineScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = DashboardWidget.Status.Hidden
        )

    private val onboardingWidgetStatus = observeOnboardingWidgetStatus()
        .stateIn(
            scope = siteCoroutineScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = DashboardWidget.Status.Hidden
        )

    val widgets = combine(
        dashboardDataStore.widgets,
        siteOrdersState,
        blazeWidgetStatus,
        onboardingWidgetStatus
    ) { widgets, siteOrdersState, blazeWidgetStatus, onboardingWidgetStatus ->
        widgets.toDomainModel(siteOrdersState, blazeWidgetStatus, onboardingWidgetStatus)
    }

    val hasNewWidgets = dashboardDataStore.widgets.map { widgets ->
        widgets.size != DashboardWidget.Type.supportedWidgets.size
    }

    suspend fun updateWidgets(widgets: List<DashboardWidget>) = dashboardDataStore.updateDashboard(
        DashboardDataModel.newBuilder()
            .addAllWidgets(widgets.map { it.toDataModel() })
            .build()
    )

    suspend fun updateWidgetVisibility(type: DashboardWidget.Type, isVisible: Boolean) {
        val dataStoreWidgets = widgets.first()
            .toMutableList()
            .apply {
                val index = indexOfFirst { it.type == type }
                if (index != -1) {
                    set(index, get(index).copy(isSelected = isVisible))
                }
            }
        updateWidgets(dataStoreWidgets)
    }

    private fun List<DashboardWidgetDataModel>.toDomainModel(
        siteOrdersState: DashboardWidget.Status,
        blazeWidgetStatus: DashboardWidget.Status,
        onboardingWidgetStatus: DashboardWidget.Status
    ): List<DashboardWidget> {
        return map { widget ->
            val type = DashboardWidget.Type.valueOf(widget.type)
            DashboardWidget(
                type = type,
                isSelected = widget.isAdded,
                status = when (type) {
                    DashboardWidget.Type.STATS,
                    DashboardWidget.Type.ORDERS,
                    DashboardWidget.Type.POPULAR_PRODUCTS -> siteOrdersState
                    DashboardWidget.Type.BLAZE -> blazeWidgetStatus
                    DashboardWidget.Type.ONBOARDING -> onboardingWidgetStatus

                    else -> DashboardWidget.Status.Available
                }
            )
        }
    }
}
