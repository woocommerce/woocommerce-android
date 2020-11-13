package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.util.StringUtils
import kotlinx.android.synthetic.main.fragment_print_shipping_label_info.view.*

class PrintShippingLabelInfoFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_print_shipping_label_info, container, false)

        view.printShippingLabelInfo_step1.text =
            StringUtils.fromHtml(getString(R.string.print_shipping_label_info_step_1))
        view.printShippingLabelInfo_step2.text =
            StringUtils.fromHtml(getString(R.string.print_shipping_label_info_step_2))
        view.printShippingLabelInfo_step3.text =
            StringUtils.fromHtml(getString(R.string.print_shipping_label_info_step_3))
        view.printShippingLabelInfo_step4.text =
            StringUtils.fromHtml(getString(R.string.print_shipping_label_info_step_4))
        view.printShippingLabelInfo_step5.text =
            StringUtils.fromHtml(getString(R.string.print_shipping_label_info_step_5))

        return view
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        activity?.let {
            it.title = getString(R.string.print_shipping_label_info_title)
        }
    }
}
