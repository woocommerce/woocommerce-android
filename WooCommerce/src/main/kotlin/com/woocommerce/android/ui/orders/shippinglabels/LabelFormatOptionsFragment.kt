package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentLabelFormatOptionsBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity

class LabelFormatOptionsFragment : BaseFragment(R.layout.fragment_label_format_options) {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentLabelFormatOptionsBinding.bind(view)
        setupToolbar(binding)
    }

    private fun setupToolbar(binding: FragmentLabelFormatOptionsBinding) {
        binding.toolbar.title = getString(R.string.print_shipping_label_format_options_title)
        binding.toolbar.navigationIcon = AppCompatResources.getDrawable(
            requireActivity(),
            R.drawable.ic_gridicons_cross_24dp
        )
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle(): String = getString(R.string.print_shipping_label_format_options_title)
}
