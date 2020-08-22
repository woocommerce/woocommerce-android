package com.woocommerce.android.ui.mystore

import com.woocommerce.android.ui.dashboard.DashboardStatsListener
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel

interface MyStoreStatsListener : DashboardStatsListener {
    fun onTopPerformerClicked(topPerformer: WCTopPerformerProductModel)
    override fun onTopEarnerClicked(topEarner: WCTopEarnerModel) {}
}
