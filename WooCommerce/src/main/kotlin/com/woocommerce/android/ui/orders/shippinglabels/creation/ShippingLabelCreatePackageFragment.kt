package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingLabelCreatePackageBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingPackageSelectorFragment.Companion.SELECTED_PACKAGE_RESULT
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShippingLabelCreatePackageFragment : BaseFragment(R.layout.fragment_shipping_label_create_package) {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: ShippingLabelCreatePackageViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentShippingLabelCreatePackageBinding.bind(view)
        val tabLayout = binding.createPackageTabLayout
        val viewPager = binding.createPackagePager

        val adapter = ShippingLabelCreatePackageViewPagerAdapter(this)
        viewPager.adapter = adapter

        initializeTabs(tabLayout, viewPager)
        setupObservers(viewModel)
    }

    private fun initializeTabs(tabLayout: TabLayout, viewPager: ViewPager2) {
        val tabArray = resources.getStringArray(R.array.shipping_label_create_new_package_tabs).toList()
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabArray[position]
        }.attach()
    }

    private fun setupObservers(viewModel: ShippingLabelCreatePackageViewModel) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitWithResult<*> -> {
                    // Once a package creation succeeds, we want to navigate right away to
                    // EditShippingLabelPackagesFragment, delivering the newly created package to be used
                    // as a selected package.
                    navigateBackWithResult(
                        SELECTED_PACKAGE_RESULT,
                        event.data,
                        R.id.editShippingLabelPackagesFragment
                    )
                }
                is ShowSnackbar -> uiMessageResolver.getSnack(
                    stringResId = event.message,
                    stringArgs = event.args
                ).show()
                else -> event.isHandled = false
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.shipping_label_create_package_title)
}
