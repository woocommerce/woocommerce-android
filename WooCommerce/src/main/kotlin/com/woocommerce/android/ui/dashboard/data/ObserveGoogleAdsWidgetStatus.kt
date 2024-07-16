package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.google.IsGoogleForWooEnabled
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveGoogleAdsWidgetStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val isGoogleForWooEnabled: IsGoogleForWooEnabled
) {
    operator fun invoke() = selectedSite.observe()
        .filterNotNull()
        .map {
            if (isGoogleForWooEnabled()) {
                DashboardWidget.Status.Available
            } else {
                DashboardWidget.Status.Hidden
            }
        }
}
