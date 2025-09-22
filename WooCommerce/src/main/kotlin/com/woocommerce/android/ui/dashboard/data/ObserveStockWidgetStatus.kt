package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.R
import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveStockWidgetStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val observePublishedProductsCount: ObservePublishedProductsCount,
    private val ciabSiteGateKeeper: CIABSiteGateKeeper
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke() = selectedSite.observe()
        .filterNotNull()
        .flatMapLatest { site ->
            if (ciabSiteGateKeeper.isFeatureUnsupported(CIABAffectedFeature.ProductsStockDashboardCard)) {
                flowOf(DashboardWidget.Status.Hidden)
            } else {
                observePublishedProductsCount().map { hasPublishedProducts ->
                    if (hasPublishedProducts) {
                        DashboardWidget.Status.Available
                    } else {
                        DashboardWidget.Status.Unavailable(
                            badgeText = R.string.my_store_widget_unavailable
                        )
                    }
                }
            }
        }
}
