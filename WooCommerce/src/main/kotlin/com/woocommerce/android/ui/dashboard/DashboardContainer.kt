package com.woocommerce.android.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.woocommerce.android.R
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.DashboardWidget.Type.BLAZE
import com.woocommerce.android.model.DashboardWidget.Type.ONBOARDING
import com.woocommerce.android.model.DashboardWidget.Type.POPULAR_PRODUCTS
import com.woocommerce.android.model.DashboardWidget.Type.STATS
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.NavigateToAddProduct
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenRangePicker
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShowPluginUnavailableError
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShowStatsError
import com.woocommerce.android.ui.dashboard.blaze.DashboardBlazeCard
import com.woocommerce.android.ui.dashboard.onboarding.DashboardOnboardingCard
import com.woocommerce.android.ui.dashboard.stats.DashboardStatsCard
import com.woocommerce.android.ui.dashboard.topperformers.DashboardTopPerformersWidgetCard
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
            .background(MaterialTheme.colors.surface)
    ) {
        widgets.forEach {
            AnimatedVisibility(it.isVisible) {
                Column {
                    when (it.type) {
                        STATS -> {
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

                        POPULAR_PRODUCTS -> DashboardTopPerformersWidgetCard(dashboardViewModel)

                        BLAZE -> DashboardBlazeCard(
                            blazeCampaignCreationDispatcher = blazeCampaignCreationDispatcher
                        )

                        ONBOARDING -> DashboardOnboardingCard(
                            parentViewModel = dashboardViewModel,
                            navigateToAddProduct = {
                                dashboardViewModel.onDashboardWidgetEvent(NavigateToAddProduct)
                            },
                        )
                    }
                }

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(id = R.dimen.major_100))
                )
            }
        }
    }
}
