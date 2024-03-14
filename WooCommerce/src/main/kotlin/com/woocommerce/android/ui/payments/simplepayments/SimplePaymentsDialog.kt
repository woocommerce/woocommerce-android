package com.woocommerce.android.ui.payments.simplepayments

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.DialogSimplePaymentsBinding
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.payments.PaymentsBaseDialogFragment
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@AndroidEntryPoint
class SimplePaymentsDialog : PaymentsBaseDialogFragment(R.layout.dialog_simple_payments) {
    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: SimplePaymentsDialogViewModel by viewModels()

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

        val binding = DialogSimplePaymentsBinding.bind(view)
        binding.buttonDone.setOnClickListener {
            viewModel.onDoneButtonClicked()
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

    private fun cancelDialog() {
        viewModel.onCancelDialogClicked()
        findNavController().navigateUp()
    }

    private fun setupObservers(binding: DialogSimplePaymentsBinding) {
        binding.editPrice.value.filterNotNull().observe(
            this
        ) {
            viewModel.currentPrice = it
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ShowSnackbar -> {
                    ActivityUtils.hideKeyboardForced(binding.editPrice)
                    uiMessageResolver.showSnack(event.message)
                }
            }
        }

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isDoneButtonEnabled.takeIfNotEqualTo(old?.isDoneButtonEnabled) { isEnabled ->
                binding.buttonDone.isEnabled = isEnabled
            }
            new.createdOrder.takeIfNotEqualTo(old?.createdOrder) { order ->
                val action = SimplePaymentsDialogDirections.actionSimplePaymentDialogToSimplePaymentFragment(
                    order!!,
                    PaymentType.SIMPLE,
                )
                findNavController().navigateSafely(action)
            }
            new.isProgressShowing.takeIfNotEqualTo(old?.isProgressShowing) { show ->
                binding.progressBar.isVisible = show
                binding.buttonDone.text = if (show) "" else getString(R.string.done)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    companion object {
        private const val HEIGHT_RATIO = 0.6
        private const val WIDTH_RATIO = 0.9
        private const val HEIGHT_RATIO_LANDSCAPE = 0.9
        private const val WIDTH_RATIO_LANDSCAPE = 0.6
        private const val KEYBOARD_DELAY = 100L
    }
}
