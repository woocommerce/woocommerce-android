package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingPackagesSelectorBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.viewmodel.ViewModelFactory
import javax.inject.Inject

class ShippingPackageSelectorFragment : BaseFragment(R.layout.fragment_shipping_packages_selector) {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ShippingPackageSelectorViewModel by viewModels { viewModelFactory }

    private val packagesAdapter by lazy { ShippingPackagesAdapter(viewModel.dimensionUnit) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentShippingPackagesSelectorBinding.bind(view)

        with(binding.packagesList) {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = packagesAdapter
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.packages.takeIfNotEqualTo(old?.packages) {
                packagesAdapter.updatePackages(it)
            }
        }
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.shipping_label_package_selector_title)
    }
}
