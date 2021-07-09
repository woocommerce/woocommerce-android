package com.woocommerce.android.ui.orders.cardreader

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentCardReaderPaymentBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.cardreader.ReceiptEvent.PrintReceipt
import com.woocommerce.android.ui.orders.cardreader.ReceiptEvent.SendReceipt
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.PrintHtmlHelper
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardReaderPaymentDialog : DialogFragment(R.layout.fragment_card_reader_payment) {
    val viewModel: CardReaderPaymentViewModel by viewModels()
    @Inject lateinit var printHtmlHelper: PrintHtmlHelper
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog!!.setCanceledOnTouchOutside(false)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel.onViewCreated()
        return object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                viewModel.onBackPressed()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCardReaderPaymentBinding.bind(view)

        initObservers(binding)
        initViewModel()
    }

    private fun initViewModel() {
        viewModel.start()
    }

    private fun initObservers(binding: FragmentCardReaderPaymentBinding) {
        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    is PrintReceipt -> printHtmlHelper.printReceipt(
                        requireActivity(),
                        event.receiptUrl,
                        event.documentName
                    )
                    is SendReceipt -> composeEmail(event.address, event.subject, event.content)
                    is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                    else -> event.isHandled = false
                }
            }
        )
        viewModel.viewStateData.observe(
            viewLifecycleOwner,
            { viewState ->
                UiHelpers.setTextOrHide(binding.headerLabel, viewState.headerLabel)
                UiHelpers.setTextOrHide(binding.amountLabel, viewState.amountWithCurrencyLabel)
                UiHelpers.setImageOrHide(binding.illustration, viewState.illustration)
                UiHelpers.setTextOrHide(binding.paymentStateLabel, viewState.paymentStateLabel)
                (binding.paymentStateLabel.layoutParams as ViewGroup.MarginLayoutParams)
                    .topMargin = resources.getDimensionPixelSize(viewState.paymentStateLabelTopMargin)
                UiHelpers.setTextOrHide(binding.hintLabel, viewState.hintLabel)
                UiHelpers.setTextOrHide(binding.primaryActionBtn, viewState.primaryActionLabel)
                UiHelpers.setTextOrHide(binding.secondaryActionBtn, viewState.secondaryActionLabel)
                UiHelpers.updateVisibility(binding.progressBarWrapper, viewState.isProgressVisible)
                binding.primaryActionBtn.setOnClickListener {
                    viewState.onPrimaryActionClicked?.invoke()
                }
                binding.secondaryActionBtn.setOnClickListener {
                    viewState.onSecondaryActionClicked?.invoke()
                }
            }
        )

        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    Exit -> {
                        navigateBackWithNotice(KEY_CARD_PAYMENT_RESULT)
                    }
                }
            }
        )
    }

    private fun composeEmail(address: String, subject: UiString, content: UiString) {
        val success = ActivityUtils.composeEmail(requireActivity(), address, subject, content)
        if (!success) viewModel.onEmailActivityNotFound()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        printHtmlHelper.getAndClearPrintJobResult()?.let {
            viewModel.onPrintResult(it)
        }
    }

    companion object {
        const val KEY_CARD_PAYMENT_RESULT = "key_card_payment_result"
    }
}
