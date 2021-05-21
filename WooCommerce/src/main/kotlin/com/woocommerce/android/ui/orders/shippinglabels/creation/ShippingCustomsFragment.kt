package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingCustomsBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShippingCustomsFragment : BaseFragment(R.layout.fragment_shipping_customs) {
    private val viewModel: ShippingCustomsViewModel by viewModels()

    private val customsAdapter: ShippingCustomsAdapter by lazy { ShippingCustomsAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentShippingCustomsBinding.bind(view)
        binding.packagesList.apply {
            this.adapter = customsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        setupObservers(binding)
    }

    private fun setupObservers(binding: FragmentShippingCustomsBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner, { old, new ->
            new.customsPackages.takeIfNotEqualTo(old?.customsPackages) { customsPackages ->
                customsAdapter.customsPackages = customsPackages
            }
        })
    }
}
