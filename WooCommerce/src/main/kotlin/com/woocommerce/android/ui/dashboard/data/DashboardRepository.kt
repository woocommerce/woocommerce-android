package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.R
import com.woocommerce.android.di.SiteComponentEntryPoint
import com.woocommerce.android.extensions.combine
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.toDataModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel
import dagger.hilt.EntryPoints
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@ActivityRetainedScoped
@Suppress("LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardRepository @Inject constructor(
    selectedSite: SelectedSite,
    private val dashboardDataStore: DashboardDataStore,
    observeSiteOrdersState: ObserveSiteOrdersState,
    observeBlazeWidgetStatus: ObserveBlazeWidgetStatus,
    observePushNotificationsWidgetStatus: ObservePushNotificationsWidgetStatus,
    observeOnboardingWidgetStatus: ObserveOnboardingWidgetStatus,
    observeStockWidgetStatus: ObserveStockWidgetStatus,
    observeGoogleAdsWidgetStatus: ObserveGoogleAdsWidgetStatus,
    observeInboxWidgetStatus: ObserveInboxWidgetStatus,
    observeAIAssistantWidgetStatus: ObserveAIAssistantWidgetStatus,
) {
    private val siteCoroutineScopeFlow = selectedSite.observe().map {
        selectedSite.siteComponent?.let { component ->
            EntryPoints.get(component, SiteComponentEntryPoint::class.java).siteCoroutineScope()
        }
    }

    private fun widgetStatusFlow(
        started: SharingStarted = SharingStarted.WhileSubscribed(),
        initialValue: DashboardWidget.Status = DashboardWidget.Status.Hidden,
        flowProvider: () -> Flow<DashboardWidget.Status>
    ) = siteCoroutineScopeFlow.flatMapLatest { scope ->
        scope?.let { flowProvider().stateIn(it, started, initialValue) } ?: flowOf(initialValue)
    }

    private val siteOrdersState = widgetStatusFlow(
        started = SharingStarted.Lazily,
        initialValue = DashboardWidget.Status.Unavailable(R.string.my_store_widget_unavailable)
    ) { observeSiteOrdersState() }

    private val blazeWidgetStatus = widgetStatusFlow { observeBlazeWidgetStatus() }

    private val pushNotificationsWidgetStatus = widgetStatusFlow { observePushNotificationsWidgetStatus() }

    private val onboardingWidgetStatus = widgetStatusFlow { observeOnboardingWidgetStatus() }

    private val stockWidgetStatus = widgetStatusFlow { observeStockWidgetStatus() }

    private val googleAdsWidgetStatus = widgetStatusFlow { observeGoogleAdsWidgetStatus() }

    private val inboxWidgetStatus = widgetStatusFlow { observeInboxWidgetStatus() }

    private val aiAssistantWidgetStatus = widgetStatusFlow { observeAIAssistantWidgetStatus() }

    val widgets = combine(
        dashboardDataStore.widgets,
        siteOrdersState,
        blazeWidgetStatus,
        pushNotificationsWidgetStatus,
        onboardingWidgetStatus,
        stockWidgetStatus,
        googleAdsWidgetStatus,
        inboxWidgetStatus,
        aiAssistantWidgetStatus
    ) { widgets, siteOrdersState, blazeWidgetStatus, pushNotificationsWidgetStatus, onboardingWidgetStatus,
        stockWidgetStatus, googleAdsWidgetStatus, inboxWidgetStatus, aiAssistantWidgetStatus ->
        widgets.toDomainModel(
            siteOrdersState,
            blazeWidgetStatus,
            pushNotificationsWidgetStatus,
            onboardingWidgetStatus,
            stockWidgetStatus,
            googleAdsWidgetStatus,
            inboxWidgetStatus,
            aiAssistantWidgetStatus
        )
    }

    val hasNewWidgets = dashboardDataStore.widgets.map { widgets ->
        widgets.size < DashboardWidget.Type.supportedWidgets.size
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

    suspend fun addNewWidgetsToTheConfig() {
        val widgets = widgets.first()
        val newWidgets = DashboardWidget.Type.supportedWidgets
            .filter { widgetType -> widgets.none { it.type == widgetType } }
            .map { widgetType ->
                DashboardWidget(
                    type = widgetType,
                    isSelected = false,
                    status = DashboardWidget.Status.Available
                )
            }
        updateWidgets(widgets + newWidgets)
    }

    suspend fun insertAIAssistantWidgetAtTopIfMissing() {
        val storedWidgets = dashboardDataStore.widgets.first()
        if (storedWidgets.any { it.type == DashboardWidget.Type.AI_ASSISTANT.name }) {
            return
        }

        val aiAssistantWidget = DashboardWidget(
            type = DashboardWidget.Type.AI_ASSISTANT,
            isSelected = true,
            status = DashboardWidget.Status.Available
        ).toDataModel()

        dashboardDataStore.updateDashboard(
            DashboardDataModel.newBuilder()
                .addWidgets(aiAssistantWidget)
                .addAllWidgets(storedWidgets)
                .build()
        )
    }

    private fun List<DashboardWidgetDataModel>.toDomainModel(
        siteOrdersState: DashboardWidget.Status,
        blazeWidgetStatus: DashboardWidget.Status,
        pushNotificationsWidgetStatus: DashboardWidget.Status,
        onboardingWidgetStatus: DashboardWidget.Status,
        stockWidgetStatus: DashboardWidget.Status,
        googleAdsWidgetStatus: DashboardWidget.Status,
        inboxWidgetStatus: DashboardWidget.Status,
        aiAssistantWidgetStatus: DashboardWidget.Status
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
                    DashboardWidget.Type.PUSH_NOTIFICATIONS -> pushNotificationsWidgetStatus
                    DashboardWidget.Type.ONBOARDING -> onboardingWidgetStatus
                    DashboardWidget.Type.STOCK -> stockWidgetStatus
                    DashboardWidget.Type.GOOGLE_ADS -> googleAdsWidgetStatus
                    DashboardWidget.Type.INBOX -> inboxWidgetStatus
                    DashboardWidget.Type.AI_ASSISTANT -> aiAssistantWidgetStatus

                    else -> DashboardWidget.Status.Available
                }
            )
        }
    }
}
