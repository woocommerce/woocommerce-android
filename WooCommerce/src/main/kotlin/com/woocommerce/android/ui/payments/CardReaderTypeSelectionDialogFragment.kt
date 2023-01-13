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

class CardReaderTypeSelectionDialogFragment : DialogFragment(R.layout.card_reader_type_selection_dialog) {
    val viewModel: CardReaderTypeSelectionViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initClicks(CardReaderTypeSelectionDialogBinding.bind(view))
        initObservers()
    }

    private fun initClicks(binding: CardReaderTypeSelectionDialogBinding) {
        binding.readerSelectionUseBlueatoothReader.setOnClickListener {
            viewModel.onUseBluetoothReaderSelected()
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
                                event.cardReaderFlowParam
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
