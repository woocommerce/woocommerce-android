package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker

class LabelFormatOptionsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_label_format_options, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        activity?.let {
            it.title = getString(R.string.print_shipping_label_format_options_title)
        }
    }
}
