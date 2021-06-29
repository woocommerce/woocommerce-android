package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingLabelCreateCustomPackageBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.CustomPackageType
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.base.BaseFragment
import org.wordpress.android.util.ActivityUtils
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreatePackageViewModel.InputName

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
            mapper = { resources.getString(it.stringRes) }
        )

        binding.customPackageFormName.setOnTextChangedListener {
            viewModel.onCustomPackageStringInputChanged(it.toString())
        }
        binding.customPackageFormLength.setOnTextChangedListener {
            viewModel.onCustomPackageFloatInputChanged(it.toString(), InputName.LENGTH)
        }
        binding.customPackageFormWidth.setOnTextChangedListener {
            viewModel.onCustomPackageFloatInputChanged(it.toString(), InputName.WIDTH)
        }
        binding.customPackageFormHeight.setOnTextChangedListener {
            viewModel.onCustomPackageFloatInputChanged(it.toString(), InputName.HEIGHT)
        }
        binding.customPackageFormEmptyWeight.setOnTextChangedListener {
            viewModel.onCustomPackageFloatInputChanged(it.toString(), InputName.EMPTY_WEIGHT)
        }

        // TODO fill metric values
    }

    private fun setupObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.customPackageType.takeIfNotEqualTo(old?.customPackageType) {
                binding.customPackageFormType.setText(getString(it.stringRes))
            }

            new.customPackageFormLengthError.takeIfNotEqualTo(old?.customPackageFormLengthError) {
                showErrorOrClear(binding.customPackageFormLength, it)
            }

            new.customPackageFormWidthError.takeIfNotEqualTo(old?.customPackageFormWidthError) {
                showErrorOrClear(binding.customPackageFormHeight, it)
            }

            new.customPackageFormHeightError.takeIfNotEqualTo(old?.customPackageFormHeightError) {
                showErrorOrClear(binding.customPackageFormLength, it)
            }

            new.customPackageFormWeightError.takeIfNotEqualTo(old?.customPackageFormWeightError) {
                showErrorOrClear(binding.customPackageFormEmptyWeight, it)
            }

            new.customPackageFormNameError.takeIfNotEqualTo(old?.customPackageFormNameError) {
                showErrorOrClear(binding.customPackageFormName, it)
            }
        }
    }

    private fun showErrorOrClear(inputLayout: TextInputLayout, @StringRes message: Int?) {
        if (message == null || message == 0) {
            inputLayout.error = null
        } else {
            inputLayout.error = resources.getString(message)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onCreateCustomPackageDoneButtonClicked(gatherData()) // TODO: for data submission
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // TODO: for data submission
    private fun gatherData(): ShippingPackage {
        val dimensions = PackageDimensions(
            binding.customPackageFormLength.getText().trim().toFloat(),
            binding.customPackageFormWidth.getText().trim().toFloat(),
            binding.customPackageFormHeight.getText().trim().toFloat(),
        )
        val isLetter =
            binding.customPackageFormType.getText() == resources.getString(CustomPackageType.ENVELOPE.stringRes)

        return ShippingPackage(
            id = "", /* Safe to set as empty, as it's not required for package creation */
            title = binding.customPackageFormName.getText(),
            isLetter = isLetter,
            category = "", /* Safe to set as empty, as it's not required for package creation */
            dimensions = dimensions,
            boxWeight = binding.customPackageFormEmptyWeight.getText().trim().toFloat()
        )
    }
}
