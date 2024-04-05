package com.woocommerce.android.ui.main

import com.woocommerce.android.R
import com.woocommerce.android.util.FeatureFlag

object DashboardDestination {
    val id = if (FeatureFlag.DYNAMIC_DASHBOARD.isEnabled()) R.id.dashboard else R.id.my_store
}
