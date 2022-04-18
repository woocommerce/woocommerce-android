package com.woocommerce.android.ui.orders.shippinglabels

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.BaseFragment

class LabelFormatOptionsFragment : BaseFragment(R.layout.fragment_label_format_options) {
    override val navigationIconForActivityToolbar: Int
        get() = R.drawable.ic_gridicons_cross_24dp

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle(): String = getString(R.string.print_shipping_label_format_options_title)
}
