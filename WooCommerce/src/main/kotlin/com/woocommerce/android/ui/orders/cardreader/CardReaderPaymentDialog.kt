package com.woocommerce.android.ui.orders.cardreader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderPaymentBinding
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.CardReaderPaymentEvent.PrintReceipt
import com.woocommerce.android.util.PrintHtmlHelper
import com.woocommerce.android.util.UiHelpers
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardReaderPaymentDialog : DialogFragment(R.layout.fragment_card_reader_payment) {
    val viewModel: CardReaderPaymentViewModel by viewModels()
    @Inject lateinit var printHtmlHelper: PrintHtmlHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog!!.setCanceledOnTouchOutside(false)
        return super.onCreateView(inflater, container, savedInstanceState)
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
        viewModel.event.observe(viewLifecycleOwner, { event ->
            when (event) {
                is PrintReceipt -> printHtmlHelper.printReceipt(requireActivity(), event.htmlReceipt, "")
                else -> event.isHandled = false
            }
        })
        viewModel.viewStateData.observe(viewLifecycleOwner, { viewState ->
            UiHelpers.setTextOrHide(binding.headerLabel, viewState.headerLabel)
            UiHelpers.setTextOrHide(binding.amountLabel, viewState.amountWithCurrencyLabel)
            UiHelpers.setImageOrHide(binding.illustration, viewState.illustration)
            UiHelpers.setTextOrHide(binding.paymentStateLabel, viewState.paymentStateLabel)
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
        })
    }
}
