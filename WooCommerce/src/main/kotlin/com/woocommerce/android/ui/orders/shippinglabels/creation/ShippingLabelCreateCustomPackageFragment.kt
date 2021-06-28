package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingLabelCreateCustomPackageBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.CustomPackageType
import com.woocommerce.android.ui.base.BaseFragment
import org.wordpress.android.util.ActivityUtils

class ShippingLabelCreateCustomPackageFragment : BaseFragment(R.layout.fragment_shipping_label_create_custom_package) {
    private var _binding: FragmentShippingLabelCreateCustomPackageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShippingLabelCreatePackageViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentShippingLabelCreateCustomPackageBinding.bind(view)

        initializeInputFields()
        setupObservers()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_done, menu)
    }


    private fun initializeInputFields() {
        // Initialize spinner
        binding.customPackageFormType.setup(
            values = CustomPackageType.values(),
            onSelected = { selectedPackage -> viewModel.onCustomPackageTypeSelected(selectedPackage) },
            mapper = { resources.getString(it.stringResource) }
        )

        // TODO fill metric values
    }

    private fun setupObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.customPackageType.takeIfNotEqualTo(old?.customPackageType) {
                binding.customPackageFormType.setText(getString(it.stringResource))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
