package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveInboxWidgetStatus @Inject constructor(
    private val selectedSite: SelectedSite
) {
    operator fun invoke() = selectedSite.observe()
        .filterNotNull()
        .map { DashboardWidget.Status.Available }
}
