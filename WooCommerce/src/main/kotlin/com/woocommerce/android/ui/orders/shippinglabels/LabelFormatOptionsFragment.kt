package com.woocommerce.android.ui.orders.shippinglabels

import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker

class LabelFormatOptionsFragment : Fragment(R.layout.fragment_label_format_options) {
    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        activity?.let {
            it.title = getString(R.string.print_shipping_label_format_options_title)
        }
    }
}
