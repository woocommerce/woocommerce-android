package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.R
import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveStockWidgetStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val observePublishedProductsCount: ObservePublishedProductsCount,
    private val ciabSiteGateKeeper: CIABSiteGateKeeper
) {
    operator fun invoke() = selectedSite.observe()
        .filterNotNull()
        .transformLatest { site ->
            if (ciabSiteGateKeeper.isFeatureUnsupported(CIABAffectedFeature.ProductsStockDashboardCard)) {
                emit(DashboardWidget.Status.Hidden)
            } else {
                emitAll(
                    observePublishedProductsCount().map { hasPublishedProducts ->
                        if (hasPublishedProducts) {
                            DashboardWidget.Status.Available
                        } else {
                            DashboardWidget.Status.Unavailable(
                                badgeText = R.string.my_store_widget_unavailable
                            )
                        }
                    }
                )
            }
        }
}
