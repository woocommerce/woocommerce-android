package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.observe
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingLabelCreateCustomPackageBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment

class ShippingLabelCreateCustomPackageFragment : BaseFragment(R.layout.fragment_shipping_label_create_custom_package) {
    private var _binding: FragmentShippingLabelCreateCustomPackageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShippingLabelCreatePackageViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentShippingLabelCreateCustomPackageBinding.bind(view)

        initializeInputFields()
        setupResultHandlers(viewModel)
        setupObservers()
    }

    private fun initializeInputFields() {
        // Initialize spinner
        binding.customPackageFormType.setClickListener { viewModel.onCustomPackageTypeSpinnerSelected() }

        // TODO fill metric values
    }

    private fun setupResultHandlers(viewModel: ShippingLabelCreatePackageViewModel) {
        handleDialogResult<ShippingLabelCustomPackageTypeDialog.ShippingLabelCustomPackageType>(
            ShippingLabelCustomPackageTypeDialog.KEY_CUSTOM_PACKAGE_TYPE_RESULT,
            R.id.shippingLabelCreatePackageFragment
        ) {
            viewModel.onCustomPackageTypeSelected(it)
        }
    }

    private fun setupObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.customPackageType.takeIfNotEqualTo(old?.customPackageType) {
                binding.customPackageFormType.setText(getString(it.stringResource))
            }

        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when(event) {
                is ViewShippingLabelCustomPackageTypesEvent -> {
                    val action = ShippingLabelCreateCustomPackageFragmentDirections
                        .actionShippingLabelCreateCustomPackageFragmentToShippingLabelCustomPackageTypeDialog(
                            event.currentPackageType
                        )
                    findNavController().navigateSafely(action)
                }
                else -> event.isHandled = false
            }
        }
    }
}
