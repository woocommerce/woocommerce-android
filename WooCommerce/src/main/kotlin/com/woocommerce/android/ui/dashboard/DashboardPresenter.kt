package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.Dispatcher
import javax.inject.Inject

class DashboardPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite
) : DashboardContract.Presenter {
    private var dashboardView: DashboardContract.View? = null

    override fun takeView(view: DashboardContract.View) {
        dashboardView = view
    }

    override fun dropView() {
        dashboardView = null
    }
}
