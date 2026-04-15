package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class ObserveInboxWidgetStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val ciabSiteGateKeeper: CIABSiteGateKeeper
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke() = selectedSite.observe()
        .filterNotNull()
        .flatMapLatest {
            if (ciabSiteGateKeeper.isFeatureUnsupported(CIABAffectedFeature.Inbox)) {
                flowOf(DashboardWidget.Status.Hidden)
            } else {
                flowOf(DashboardWidget.Status.Available)
            }
        }
}
