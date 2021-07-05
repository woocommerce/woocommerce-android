package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingLabelCreateCustomPackageBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.CustomPackageType
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import org.wordpress.android.util.ActivityUtils
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreateCustomPackageViewModel.InputName
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreateCustomPackageViewModel.PackageSuccessfullyMadeEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShippingLabelCreateCustomPackageFragment : BaseFragment(R.layout.fragment_shipping_label_create_custom_package) {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var progressDialog: CustomProgressDialog? = null
    private var _binding: FragmentShippingLabelCreateCustomPackageBinding? = null
    private val binding get() = _binding!!

    // Needed for navigating back
    private val parentViewModel: ShippingLabelCreatePackageViewModel by viewModels({ requireParentFragment() })

    private val viewModel: ShippingLabelCreateCustomPackageViewModel by viewModels()

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
            viewModel.sanitizeStringInput(it.toString())
        }
        binding.customPackageFormLength.setOnTextChangedListener {
            viewModel.sanitizeFloatInput(it.toString(), InputName.LENGTH)
        }
        binding.customPackageFormWidth.setOnTextChangedListener {
            viewModel.sanitizeFloatInput(it.toString(), InputName.WIDTH)
        }
        binding.customPackageFormHeight.setOnTextChangedListener {
            viewModel.sanitizeFloatInput(it.toString(), InputName.HEIGHT)
        }
        binding.customPackageFormEmptyWeight.setOnTextChangedListener {
            viewModel.sanitizeFloatInput(it.toString(), InputName.EMPTY_WEIGHT)
        }

        // Fill proper units
        binding.customPackageFormLength.hint = binding.root.context.getString(
            R.string.shipping_label_create_custom_package_field_length,
            viewModel.dimensionUnit
        )
        binding.customPackageFormWidth.hint = binding.root.context.getString(
            R.string.shipping_label_create_custom_package_field_width,
            viewModel.dimensionUnit
        )
        binding.customPackageFormHeight.hint = binding.root.context.getString(
            R.string.shipping_label_create_custom_package_field_height,
            viewModel.dimensionUnit
        )
        binding.customPackageFormEmptyWeight.hint = binding.root.context.getString(
            R.string.shipping_label_create_custom_package_field_empty_weight,
            viewModel.weightUnit
        )
    }

    private fun setupObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.customFormType.takeIfNotEqualTo(old?.customFormType) {
                binding.customPackageFormType.setText(getString(it.stringRes))
            }

            new.customFormLengthError.takeIfNotEqualTo(old?.customFormLengthError) {
                showErrorOrClear(binding.customPackageFormLength, it)
            }

            new.customFormWidthError.takeIfNotEqualTo(old?.customFormWidthError) {
                showErrorOrClear(binding.customPackageFormWidth, it)
            }

            new.customFormHeightError.takeIfNotEqualTo(old?.customFormHeightError) {
                showErrorOrClear(binding.customPackageFormHeight, it)
            }

            new.customFormWeightError.takeIfNotEqualTo(old?.customFormWeightError) {
                showErrorOrClear(binding.customPackageFormEmptyWeight, it)
            }

            new.customFormNameError.takeIfNotEqualTo(old?.customFormNameError) {
                showErrorOrClear(binding.customPackageFormName, it)
            }

            new.isSavingProgressDialogVisible?.takeIfNotEqualTo(old?.isSavingProgressDialogVisible) { isVisible ->
                if(isVisible) {
                    showProgressDialog(
                        title = R.string.shipping_label_create_custom_package_saving_progress_title,
                        message = R.string.shipping_label_create_custom_package_saving_progress_message
                    )
                } else {
                    hideProgressDialog()
                }
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is PackageSuccessfullyMadeEvent -> parentViewModel.onPackageCreated(event.madePackage)
                is ShowSnackbar -> uiMessageResolver.getSnack(event.message, *event.args).show()
                else -> event.isHandled = false
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

    private fun showProgressDialog(@StringRes title: Int, @StringRes message: Int) {
        hideProgressDialog()
        progressDialog = CustomProgressDialog.show(
            getString(title),
            getString(message)
        ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onCustomFormDoneMenuClicked(
                    binding.customPackageFormType.getText(),
                    binding.customPackageFormName.getText(),
                    binding.customPackageFormLength.getText(),
                    binding.customPackageFormWidth.getText(),
                    binding.customPackageFormHeight.getText(),
                    binding.customPackageFormEmptyWeight.getText()
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
