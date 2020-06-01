package com.woocommerce.android.ui.products.categories

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.products.BaseProductFragment

class ProductCategoriesFragment : BaseProductFragment() {
    override fun getFragmentTitle() = getString(R.string.product_categories)

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onRequestAllowBackPress(): Boolean {
        // TODO:
        return true
    }
}
