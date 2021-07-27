package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogMoveShippingItemBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.getTitle
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationPackage.ExistingPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationPackage.NewPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationPackage.OriginalPackage
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MoveShippingItemDialog : DialogFragment(R.layout.dialog_move_shipping_item) {
    companion object {
        const val MOVE_ITEM_RESULT = "move-item-result"
    }

    private val viewModel: MoveShippingItemViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = DialogMoveShippingItemBinding.bind(view)
        initUi(binding)
        setupObservers(binding)
    }

    private fun initUi(binding: DialogMoveShippingItemBinding) {
        binding.dialogDescription.text =
            getString(R.string.shipping_label_move_item_dialog_description, viewModel.currentPackage.description)

        with(viewModel.availableDestinations) {
            forEach {
                binding.optionsGroup.addView(it.generateRadioButton())
            }
        }

        binding.moveButton.setOnClickListener {
            viewModel.onMoveButtonClicked()
        }
        binding.cancelButton.setOnClickListener {
            viewModel.onCancelButtonClicked()
        }
    }

    private fun setupObservers(binding: DialogMoveShippingItemBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.selectedDestination?.takeIfNotEqualTo(old?.selectedDestination) { selectedItem ->
                binding.optionsGroup.children.firstOrNull { it.tag == selectedItem }?.isSelected = true
            }
            new.isMoveButtonEnabled.takeIfNotEqualTo(old?.isMoveButtonEnabled) {
                binding.moveButton.isEnabled = it
            }
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitWithResult<*> -> navigateBackWithResult(MOVE_ITEM_RESULT, event.data)
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }
    }

    private fun DestinationPackage.generateRadioButton(): RadioButton {
        return RadioButton(requireContext()).apply {
            text = when (this@generateRadioButton) {
                is ExistingPackage -> destinationPackage.description
                NewPackage -> getString(R.string.shipping_label_move_item_dialog_new_package_option)
                OriginalPackage -> getString(R.string.shipping_label_move_item_dialog_original_packaging_option)
            }
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.onDestinationPackageSelected(this@generateRadioButton)
                }
            }
            tag = this@generateRadioButton
        }
    }

    private val ShippingLabelPackage.description
        get() = if (selectedPackage == null) getTitle(requireContext())
        else "${getTitle(requireContext())}: ${selectedPackage.title}"
}
