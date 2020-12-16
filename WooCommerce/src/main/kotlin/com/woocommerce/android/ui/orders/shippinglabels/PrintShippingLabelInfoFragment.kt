package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentPrintShippingLabelInfoBinding
import com.woocommerce.android.util.StringUtils

class PrintShippingLabelInfoFragment : Fragment(R.layout.fragment_print_shipping_label_info) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPrintShippingLabelInfoBinding.bind(view)
        binding.printShippingLabelInfoStep1.text =
            StringUtils.fromHtml(getString(R.string.print_shipping_label_info_step_1))
        binding.printShippingLabelInfoStep2.text =
            StringUtils.fromHtml(getString(R.string.print_shipping_label_info_step_2))
        binding.printShippingLabelInfoStep3.text =
            StringUtils.fromHtml(getString(R.string.print_shipping_label_info_step_3))
        binding.printShippingLabelInfoStep4.text =
            StringUtils.fromHtml(getString(R.string.print_shipping_label_info_step_4))
        binding.printShippingLabelInfoStep5.text =
            StringUtils.fromHtml(getString(R.string.print_shipping_label_info_step_5))
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        activity?.let {
            it.title = getString(R.string.print_shipping_label_info_title)
        }
    }
}
