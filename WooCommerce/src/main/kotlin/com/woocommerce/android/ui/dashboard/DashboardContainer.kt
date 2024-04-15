package com.woocommerce.android.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenRangePicker
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShowPluginUnavailableError
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShowStatsError
import com.woocommerce.android.ui.dashboard.blaze.DashboardBlazeCard
import com.woocommerce.android.ui.dashboard.stats.DashboardStatsCard
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils

@Composable
fun DashboardContainer(
    dashboardViewModel: DashboardViewModel,
    dateUtils: DateUtils,
    currencyFormatter: CurrencyFormatter,
    usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher
) {
    dashboardViewModel.dashboardWidgets.observeAsState().value?.let { widgets ->
        WidgetList(
            dashboardViewModel = dashboardViewModel,
            widgets = widgets,
            dateUtils = dateUtils,
            currencyFormatter = currencyFormatter,
            usageTracksEventEmitter = usageTracksEventEmitter,
            blazeCampaignCreationDispatcher = blazeCampaignCreationDispatcher
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun WidgetList(
    dashboardViewModel: DashboardViewModel,
    widgets: List<DashboardWidget>,
    dateUtils: DateUtils,
    currencyFormatter: CurrencyFormatter,
    usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        widgets.forEach {
            AnimatedVisibility(it.isVisible) {
                Column {
                    WidgetCard(
                        modifier = Modifier
                            .animateEnterExit(enter = slideInVertically(), exit = slideOutVertically())
                    ) {
                        when (it.type) {
                            DashboardWidget.Type.STATS -> {
                                DashboardStatsCard(
                                    dateUtils = dateUtils,
                                    currencyFormatter = currencyFormatter,
                                    usageTracksEventEmitter = usageTracksEventEmitter,
                                    onPluginUnavailableError = {
                                        dashboardViewModel.onDashboardWidgetEvent(ShowPluginUnavailableError)
                                    },
                                    onStatsError = {
                                        dashboardViewModel.onDashboardWidgetEvent(ShowStatsError)
                                    },
                                    openDatePicker = { start, end, callback ->
                                        dashboardViewModel.onDashboardWidgetEvent(OpenRangePicker(start, end, callback))
                                    },
                                    parentViewModel = dashboardViewModel
                                )
                            }

                            DashboardWidget.Type.POPULAR_PRODUCTS -> {}
                            DashboardWidget.Type.BLAZE -> DashboardBlazeCard(
                                blazeCampaignCreationDispatcher = blazeCampaignCreationDispatcher
                            )

                            DashboardWidget.Type.ONBOARDING -> {}
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier) {
        content()
    }
}
