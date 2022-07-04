package com.woocommerce.android.ui.cardreader.payment

import android.app.Dialog
import android.content.ContentResolver
import android.media.MediaPlayer
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.CardReaderPaymentDialogBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.cardreader.payment.CardReaderPaymentViewModel.ShowSnackbarInDialog
import com.woocommerce.android.ui.cardreader.receipt.ReceiptEvent.PrintReceipt
import com.woocommerce.android.ui.cardreader.receipt.ReceiptEvent.SendReceipt
import com.woocommerce.android.ui.refunds.RefundSummaryFragment.Companion.KEY_INTERAC_SUCCESS
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.PrintHtmlHelper
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardReaderPaymentDialogFragment : DialogFragment(R.layout.card_reader_payment_dialog) {
    val viewModel: CardReaderPaymentViewModel by viewModels()

    @Inject lateinit var printHtmlHelper: PrintHtmlHelper

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.let {
            it.setCanceledOnTouchOutside(false)
            it.requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
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
        val binding = CardReaderPaymentDialogBinding.bind(view)
        initObservers(binding)
        initViewModel()
    }

    private fun initViewModel() {
        viewModel.start()
    }

    private fun initObservers(binding: CardReaderPaymentDialogBinding) {
        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is PrintReceipt -> printHtmlHelper.printReceipt(
                    requireActivity(),
                    event.receiptUrl,
                    event.documentName
                )
                CardReaderPaymentViewModel.InteracRefundSuccessful -> navigateBackWithNotice(KEY_INTERAC_SUCCESS)
                is SendReceipt -> composeEmail(event.address, event.subject, event.content)
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowSnackbarInDialog -> Snackbar.make(
                    requireView(), event.message, BaseTransientBottomBar.LENGTH_LONG
                ).show()
                is CardReaderPaymentViewModel.PlayChaChing -> playChaChing()
                else -> event.isHandled = false
            }
        }
        viewModel.viewStateData.observe(
            viewLifecycleOwner
        ) { viewState ->
            announceForAccessibility(binding, viewState)
            UiHelpers.setTextOrHide(binding.headerLabel, viewState.headerLabel)
            UiHelpers.setTextOrHide(binding.amountLabel, viewState.amountWithCurrencyLabel)
            UiHelpers.setImageOrHideInLandscape(binding.illustration, viewState.illustration)
            UiHelpers.setTextOrHide(binding.paymentStateLabel, viewState.paymentStateLabel)
            (binding.paymentStateLabel.layoutParams as ViewGroup.MarginLayoutParams)
                .topMargin = resources.getDimensionPixelSize(viewState.paymentStateLabelTopMargin)
            UiHelpers.setTextOrHide(binding.hintLabel, viewState.hintLabel)
            UiHelpers.setTextOrHide(binding.primaryActionBtn, viewState.primaryActionLabel)
            UiHelpers.setTextOrHide(binding.secondaryActionBtn, viewState.secondaryActionLabel)
            UiHelpers.setTextOrHide(binding.tertiaryActionBtn, viewState.tertiaryActionLabel)
            UiHelpers.setTextOrHide(binding.receiptSentLabel, viewState.receiptSentAutomaticallyHint)
            UiHelpers.updateVisibility(binding.progressBarWrapper, viewState.isProgressVisible)
            binding.primaryActionBtn.setOnClickListener {
                viewState.onPrimaryActionClicked?.invoke()
            }
            binding.secondaryActionBtn.setOnClickListener {
                viewState.onSecondaryActionClicked?.invoke()
            }
            binding.tertiaryActionBtn.setOnClickListener {
                viewState.onTertiaryActionClicked?.invoke()
            }
        }

        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                Exit -> {
                    navigateBackWithNotice(KEY_CARD_PAYMENT_RESULT)
                }
            }
        }
    }

    private fun announceForAccessibility(binding: CardReaderPaymentDialogBinding, viewState: ViewState) {
        with(binding) {
            if (viewState is ViewState.PaymentSuccessfulState ||
                viewState is ViewState.PaymentSuccessfulReceiptSentAutomaticallyState
            ) {
                viewState.headerLabel?.let {
                    headerLabel.announceForAccessibility(getString(it) + viewState.amountWithCurrencyLabel)
                }
            } else {
                viewState.paymentStateLabel?.let {
                    paymentStateLabel.announceForAccessibility(getString(it))
                }
            }
            viewState.hintLabel?.let {
                hintLabel.announceForAccessibility(getString(it))
            }
        }
    }

    private fun playChaChing() {
        val chaChingUri =
            Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE +
                    "://" + requireActivity().packageName + "/" + R.raw.cha_ching
            )
        val mp = MediaPlayer.create(requireActivity(), chaChingUri)
        mp.start()
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
        disableDigitalWallets()
    }

    override fun onPause() {
        super.onPause()
        reEnableDigitalWallets()
    }

    /**
     * Disables digital wallets (eg. Google Pay) in order to prevent the merchant from accidentally charging themselves
     * instead of the customer.
     */
    private fun disableDigitalWallets() {
        NfcAdapter.getDefaultAdapter(requireContext())
            ?.enableReaderMode(
                requireActivity(),
                { },
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
    }

    private fun reEnableDigitalWallets() {
        NfcAdapter.getDefaultAdapter(requireContext())
            ?.disableReaderMode(requireActivity())
    }

    companion object {
        const val KEY_CARD_PAYMENT_RESULT = "key_card_payment_result"
    }
}
