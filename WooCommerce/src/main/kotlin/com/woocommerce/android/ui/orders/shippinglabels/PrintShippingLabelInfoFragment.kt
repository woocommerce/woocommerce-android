package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentPrintShippingLabelInfoBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.StringUtils

class PrintShippingLabelInfoFragment : BaseFragment(R.layout.fragment_print_shipping_label_info) {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = R.drawable.ic_gridicons_cross_24dp
        )

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
    }

    override fun getFragmentTitle(): String = getString(R.string.print_shipping_label_info_title)
}
