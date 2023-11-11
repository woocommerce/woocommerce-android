package com.woocommerce.android.ui.payments.customamounts

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
            sharedViewModel.onCustomAmountUpsert(
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
        Handler(Looper.getMainLooper()).postDelayed(
            {
                binding.editPrice.value.filterNotNull().observe(
                    this
                ) {
                    viewModel.currentPrice = it
                }
            },
            EDIT_PRICE_UPDATE_DELAY
        )

        binding.customAmountNameText.addTextChangedListener {
            viewModel.currentName = it.toString()
        }

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isDoneButtonEnabled.takeIfNotEqualTo(old?.isDoneButtonEnabled) { isEnabled ->
                binding.buttonDone.isEnabled = isEnabled
            }

            new.isProgressShowing.takeIfNotEqualTo(old?.isProgressShowing) { show ->
                binding.progressBar.isVisible = show
                binding.buttonDone.text = if (show) "" else getString(R.string.custom_amounts_add_custom_amount)
            }
            new.customAmountUIModel.takeIfNotEqualTo(old?.customAmountUIModel) {
                if (binding.customAmountNameText.text.toString() != it.name) {
                    binding.customAmountNameText.setText(it.name)
                    binding.customAmountNameText.setSelection(it.name.length)
                }
                if (binding.editPrice.editText.text.toString() != it.currentPrice.toString()) {
                    binding.editPrice.setValue(it.currentPrice)
                    binding.editPrice.editText.setSelection(binding.editPrice.editText.text?.length ?: 0)
                }
            }
        }
    }

    private fun cancelDialog() {
        findNavController().navigateUp()
    }

    companion object {
        private const val HEIGHT_RATIO = 0.6
        private const val WIDTH_RATIO = 0.9
        private const val HEIGHT_RATIO_LANDSCAPE = 0.9
        private const val WIDTH_RATIO_LANDSCAPE = 0.6
        private const val KEYBOARD_DELAY = 100L

        const val CUSTOM_AMOUNT = "Custom Amount"
        const val EDIT_PRICE_UPDATE_DELAY = 100L
    }
}
