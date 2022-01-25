package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingLabelCreateCustomPackageBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.CustomPackageType
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreateCustomPackageViewModel.InputName
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreateCustomPackageViewModel.PackageSuccessfullyMadeEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
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
        binding.typeSpinner.setup(
            values = CustomPackageType.values(),
            onSelected = { selectedPackage -> viewModel.onCustomPackageTypeSelected(selectedPackage) },
            mapper = { resources.getString(it.stringRes) }
        )

        binding.name.setOnTextChangedListener {
            viewModel.onFieldTextChanged(it.toString(), InputName.NAME)
        }
        binding.length.setOnTextChangedListener {
            viewModel.onFieldTextChanged(it.toString(), InputName.LENGTH)
        }
        binding.width.setOnTextChangedListener {
            viewModel.onFieldTextChanged(it.toString(), InputName.WIDTH)
        }
        binding.height.setOnTextChangedListener {
            viewModel.onFieldTextChanged(it.toString(), InputName.HEIGHT)
        }
        binding.weight.setOnTextChangedListener {
            viewModel.onFieldTextChanged(it.toString(), InputName.EMPTY_WEIGHT)
        }

        // Fill proper units
        binding.length.hint = binding.root.context.getString(
            R.string.shipping_label_create_custom_package_field_length,
            viewModel.dimensionUnit
        )
        binding.width.hint = binding.root.context.getString(
            R.string.shipping_label_create_custom_package_field_width,
            viewModel.dimensionUnit
        )
        binding.height.hint = binding.root.context.getString(
            R.string.shipping_label_create_custom_package_field_height,
            viewModel.dimensionUnit
        )
        binding.weight.hint = binding.root.context.getString(
            R.string.shipping_label_create_custom_package_field_empty_weight,
            viewModel.weightUnit
        )
    }

    private fun setupObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.type.takeIfNotEqualTo(old?.type) {
                binding.typeSpinner.setText(getString(it.stringRes))
            }

            new.lengthErrorMessage.takeIfNotEqualTo(old?.lengthErrorMessage) { error ->
                binding.length.error = error?.let { getString(it) }
            }

            new.widthErrorMessage.takeIfNotEqualTo(old?.widthErrorMessage) { error ->
                binding.width.error = error?.let { getString(it) }
            }

            new.heightErrorMessage.takeIfNotEqualTo(old?.heightErrorMessage) { error ->
                binding.height.error = error?.let { getString(it) }
            }

            new.weightErrorMessage.takeIfNotEqualTo(old?.weightErrorMessage) { error ->
                binding.weight.error = error?.let { getString(it) }
            }

            new.nameErrorMessage.takeIfNotEqualTo(old?.nameErrorMessage) { error ->
                binding.name.error = error?.let { getString(it) }
            }

            new.isSavingProgressDialogVisible?.takeIfNotEqualTo(old?.isSavingProgressDialogVisible) { isVisible ->
                if (isVisible) {
                    showProgressDialog(
                        title = R.string.shipping_label_create_custom_package_saving_progress_title,
                        message = R.string.shipping_label_create_package_saving_progress_message
                    )
                } else {
                    hideProgressDialog()
                }
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is PackageSuccessfullyMadeEvent -> parentViewModel.onPackageCreated(event.madePackage)
                is ShowSnackbar -> uiMessageResolver.getSnack(
                    stringResId = event.message,
                    stringArgs = event.args
                ).show()
                else -> event.isHandled = false
            }
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
                viewModel.onCustomFormDoneMenuClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
