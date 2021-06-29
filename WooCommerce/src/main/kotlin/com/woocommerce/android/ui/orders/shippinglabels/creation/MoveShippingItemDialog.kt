package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.databinding.DialogMoveShippingItemBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.getTitle
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationItem
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationItem.ExistingPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationItem.NewPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationItem.OriginalPackage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MoveShippingItemDialog : DialogFragment(R.layout.dialog_move_shipping_item) {
    private val viewModel: MoveShippingItemViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DialogMoveShippingItemBinding.bind(view)
        initUi(binding)
        setupObservers(binding)
    }

    private fun initUi(binding: DialogMoveShippingItemBinding) {
        val packageDescription = viewModel.currentPackage.getTitle(requireContext()) +
            viewModel.currentPackage.selectedPackage?.let { ": ${it.title}" }
        binding.dialogDescription.text =
            getString(string.shipping_label_move_item_dialog_description, packageDescription)

        with(viewModel.availableDestinations) {
            forEach {
                binding.optionsGroup.addView(it.generateRadioButton())
            }
        }
    }

    private fun setupObservers(binding: DialogMoveShippingItemBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner, { old, new ->
            new.selectedDestination?.takeIfNotEqualTo(old?.selectedDestination) { selectedItem ->
                binding.optionsGroup.children.firstOrNull { it.tag == selectedItem }?.isSelected = true
            }
            new.isMoveButtonEnabled.takeIfNotEqualTo(old?.isMoveButtonEnabled) {
                binding.moveButton.isEnabled = it
            }
        })
    }

    private fun DestinationItem.generateRadioButton(): RadioButton {
        return RadioButton(requireContext()).apply {
            setText(
                when (this@generateRadioButton) {
                    is ExistingPackage -> TODO()
                    NewPackage -> R.string.shipping_label_move_item_dialog_new_package_option
                    OriginalPackage -> R.string.shipping_label_move_item_dialog_original_packaging_option
                }
            )
            tag = this@generateRadioButton
        }
    }
}
