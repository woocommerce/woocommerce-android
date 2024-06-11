package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveBlazeWidgetStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val isBlazeEnabled: IsBlazeEnabled,
    private val observePublishedProductsCount: ObservePublishedProductsCount
) {
    operator fun invoke() = selectedSite.observe()
        .filterNotNull()
        .map { isBlazeEnabled() }
        .combine(observePublishedProductsCount()) { isBlazeEnabled, hasPublishedProducts ->
            if (isBlazeEnabled && hasPublishedProducts) {
                DashboardWidget.Status.Available
            } else {
                DashboardWidget.Status.Hidden
            }
        }
}
