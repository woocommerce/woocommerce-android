package com.woocommerce.android.ui.payments.customamounts

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogCustomAmountsBinding
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.creation.CustomAmountUIModel
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.payments.PaymentsBaseDialogFragment
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@AndroidEntryPoint
class CustomAmountsDialog : PaymentsBaseDialogFragment(R.layout.dialog_custom_amounts) {
    @Inject
    lateinit var currencyFormatter: CurrencyFormatter
    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: CustomAmountsDialogViewModel by viewModels()
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreateEditViewModel>(R.id.nav_graph_order_creations)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = ComponentDialog(requireContext(), theme)
        dialog.onBackPressedDispatcher.addCallback(dialog) {
            cancelDialog()
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isLandscape = DisplayUtils.isLandscape(requireContext())
        requireDialog().window?.let { window ->
            window.attributes?.windowAnimations = R.style.Woo_Animations_Dialog
            val widthRatio = if (isLandscape) WIDTH_RATIO_LANDSCAPE else WIDTH_RATIO
            val heightRatio = if (isLandscape) HEIGHT_RATIO_LANDSCAPE else HEIGHT_RATIO

            window.setLayout(
                (DisplayUtils.getWindowPixelWidth(requireContext()) * widthRatio).toInt(),
                (DisplayUtils.getWindowPixelHeight(requireContext()) * heightRatio).toInt()
            )
        }

        val binding = DialogCustomAmountsBinding.bind(view)
        binding.buttonDone.setOnClickListener {
            sharedViewModel.onCustomAmountAdd(
                CustomAmountUIModel(
                    id = viewModel.viewState.customAmountUIModel.id,
                    amount = viewModel.viewState.customAmountUIModel.currentPrice,
                    name = viewModel.viewState.customAmountUIModel.name
                )
            )
        }
        binding.imageClose.setOnClickListener {
            cancelDialog()
        }

        if (!isLandscape && binding.editPrice.editText.requestFocus()) {
            binding.editPrice.postDelayed(
                {
                    ActivityUtils.showKeyboard(binding.editPrice.editText)
                },
                KEYBOARD_DELAY
            )
        }
        setupObservers(binding)
    }

    private fun setupObservers(binding: DialogCustomAmountsBinding) {
        binding.editPrice.value.filterNotNull().observe(
            this
        ) {
            viewModel.currentPrice = it
        }

        binding.customAmountNameText.addTextChangedListener {
            viewModel.currentName = it.toString()
        }

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isDoneButtonEnabled.takeIfNotEqualTo(old?.isDoneButtonEnabled) { isEnabled ->
                binding.buttonDone.isEnabled = isEnabled
            }

            new.isProgressShowing.takeIfNotEqualTo(old?.isProgressShowing) { show ->
                binding.progressBar.isVisible = show
                binding.buttonDone.text = if (show) "" else getString(R.string.done)
            }
        }
    }

    private fun cancelDialog() {
        viewModel.onCancelDialogClicked()
        findNavController().navigateUp()
    }

    companion object {
        private const val HEIGHT_RATIO = 0.6
        private const val WIDTH_RATIO = 0.9
        private const val HEIGHT_RATIO_LANDSCAPE = 0.9
        private const val WIDTH_RATIO_LANDSCAPE = 0.6
        private const val KEYBOARD_DELAY = 100L

        const val CUSTOM_AMOUNT = "Custom Amount"
    }
}
