package com.woocommerce.android.ui.payments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.CardReaderTypeSelectionDialogBinding
import com.woocommerce.android.ui.payments.CardReaderTypeSelectionViewModel.NavigateToCardReaderPaymentFlow
import com.woocommerce.android.util.UiHelpers
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderTypeSelectionDialogFragment : DialogFragment(R.layout.card_reader_type_selection_dialog) {
    val viewModel: CardReaderTypeSelectionViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setIllustration(view)
        initClicks(CardReaderTypeSelectionDialogBinding.bind(view))
        initObservers()
    }

    private fun setIllustration(view: View) {
        val binding = CardReaderTypeSelectionDialogBinding.bind(view)
        UiHelpers.setImageOrHideInLandscape(
            binding.cardReaderTypeSelectionIllustration,
            R.drawable.img_ipp_reader_type_selection
        )
    }

    private fun initClicks(binding: CardReaderTypeSelectionDialogBinding) {
        binding.readerSelectionUseBluetoothReader.setOnClickListener {
            viewModel.onUseBluetoothReaderSelected()
        }
        binding.readerSelectionUseBuiltInReader.setOnClickListener {
            viewModel.onUseTapToPaySelected()
        }
    }

    private fun initObservers() {
        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is NavigateToCardReaderPaymentFlow -> {
                    val action =
                        CardReaderTypeSelectionDialogFragmentDirections
                            .actionCardReaderTypeSelectionDialogFragmentToCardReaderConnectDialogFragment(
                                event.cardReaderFlowParam,
                                event.cardReaderType,
                            )
                    findNavController().navigate(action)
                }
                else -> event.isHandled = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
