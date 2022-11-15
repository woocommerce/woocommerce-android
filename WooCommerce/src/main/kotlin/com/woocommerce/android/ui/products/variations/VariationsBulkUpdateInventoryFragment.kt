package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentVariationsBulkUpdateInventoryBinding
import com.woocommerce.android.extensions.showKeyboardWithDelay
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.products.variations.ValuesGroupType.Common
import com.woocommerce.android.ui.products.variations.ValuesGroupType.Mixed
import com.woocommerce.android.ui.products.variations.ValuesGroupType.None
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.ActivityUtils.hideKeyboard
import javax.inject.Inject

@AndroidEntryPoint
class VariationsBulkUpdateInventoryFragment :
    BaseFragment(R.layout.fragment_variations_bulk_update_inventory) {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: VariationsBulkUpdateInventoryViewModel by viewModels()

    private var _binding: FragmentVariationsBulkUpdateInventoryBinding? = null
    private val binding get() = _binding!!

    private var progressDialog: CustomProgressDialog? = null

    private var doneMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentVariationsBulkUpdateInventoryBinding.bind(view)
        binding.stockQuantityEditText.run {
            editText?.showKeyboardWithDelay()
            setOnTextChangedListener { rawQuantity ->
                viewModel.onStockQuantityChanged(rawQuantity.toString())
            }
        }

        observeViewStateChanges()
        observeEvents()
        setupMenu()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_variations_bulk_update, menu)
                    doneMenuItem = menu.findItem(R.id.done)
                }

                override fun onMenuItemSelected(item: MenuItem): Boolean {
                    return when (item.itemId) {
                        R.id.done -> {
                            viewModel.onDoneClicked()
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> {
                    ActivityUtils.hideKeyboardForced(binding.stockQuantityEditText.editText)
                    uiMessageResolver.showSnack(event.message)
                }
                is Exit -> requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun observeViewStateChanges() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.stockQuantity?.takeIfNotEqualTo(old?.stockQuantity) {
                val quantity = StringUtils.formatCountDecimal(it, forInput = true)
                binding.stockQuantityEditText.setTextIfDifferent(quantity)
            }
            new.variationsToUpdateCount?.takeIfNotEqualTo(old?.variationsToUpdateCount) {
                binding.currentStockQuantity.text =
                    getString(R.string.variations_bulk_update_stock_quantity_info).format(new.variationsToUpdateCount)
            }
            new.stockQuantityGroupType?.takeIfNotEqualTo(old?.stockQuantityGroupType) {
                updateCurrentStockQuantityLabel(new.stockQuantityGroupType, new)
            }
            new.isProgressDialogShown.takeIfNotEqualTo(old?.isProgressDialogShown) { isVisible ->
                updateProgressbarDialogVisibility(isVisible)
            }
            new.isDoneEnabled.takeIfNotEqualTo(old?.isDoneEnabled) { isEnabled ->
                doneMenuItem?.isEnabled = isEnabled
            }
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun updateCurrentStockQuantityLabel(
        groupType: ValuesGroupType,
        viewState: VariationsBulkUpdateInventoryViewModel.ViewState
    ) {
        binding.currentStockQuantity.text = when (groupType) {
            Mixed -> getString(R.string.variations_bulk_update_current_stock_quantity_mixed)
            None -> ""
            is Common -> getString(R.string.variations_bulk_update_current_stock_quantity, viewState.stockQuantity)
        }
    }

    private fun updateProgressbarDialogVisibility(visible: Boolean) {
        if (visible) {
            hideProgressDialog()
            progressDialog = CustomProgressDialog.show(
                getString(R.string.variations_bulk_update_stock_quantity_dialog_title),
                getString(R.string.product_update_dialog_message)
            ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        hideProgressDialog()
        hideKeyboard(requireActivity())
    }

    override fun getFragmentTitle() = getString(R.string.product_stock_quantity)
}
