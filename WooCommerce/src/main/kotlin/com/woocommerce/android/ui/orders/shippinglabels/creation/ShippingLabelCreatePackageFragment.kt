package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingLabelCreatePackageBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreatePackageViewModel.PackageType


class ShippingLabelCreatePackageFragment: BaseFragment(R.layout.fragment_shipping_label_create_package) {
    private var _binding: FragmentShippingLabelCreatePackageBinding? = null
    private val binding get() = _binding!!

    private var _tabLayout: TabLayout? = null
    private val tabLayout get() = _tabLayout!!

    private var _viewPager: ViewPager2? = null
    private val viewPager get() = _viewPager!!

    private val viewModel: ShippingLabelCreatePackageViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentShippingLabelCreatePackageBinding.bind(view)
        _tabLayout = binding.createPackageTabLayout
        _viewPager = binding.createPackagePager

        initializeTabs()

        val adapter = ShippingLabelCreatePackageViewPagerAdapter(this, PackageType.values().size)
        viewPager.adapter = adapter

        setupObservers(viewModel)
    }

    private fun initializeTabs() {
        // Get the english version to use for setting the tab tag.
        val englishTabArray = StringUtils
            .getStringArrayByLocale(requireContext(), R.array.shipping_label_create_new_package_tabs, "en")

       val tabArray = resources.getStringArray(R.array.shipping_label_create_new_package_tabs).toList()
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabArray[position]
            tab.tag = englishTabArray?.get(position) ?: tabArray[position]
        }.attach()
    }

    private fun setupObservers(viewModel: ShippingLabelCreatePackageViewModel) {
        // TODO
    }

    override fun onDestroyView() {
        _tabLayout = null
        _viewPager = null
        _binding = null
        super.onDestroyView()
    }
}
