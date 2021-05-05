package com.woocommerce.android.ui.prefs.cardreader.connect

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.WooCommerce
import com.woocommerce.android.databinding.CardReaderConnectBinding
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.InitializeCardReaderManager
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderConnectFragment : DialogFragment(R.layout.card_reader_connect) {
    val viewModel: CardReaderConnectViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = CardReaderConnectBinding.bind(view)
        initObservers(binding)
        (requireActivity().application as? WooCommerce)?.cardReaderManager?.let {
            viewModel.start(it)
        } ?: throw IllegalStateException("CardReaderManager is null.")
    }

    private fun initObservers(binding: CardReaderConnectBinding) {
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is InitializeCardReaderManager -> {
                    it.cardReaderManager.initialize(requireActivity().application)
                    viewModel.onCardReaderManagerInitialized()
                }
                is Exit -> findNavController().navigateUp()
                else -> it.isHandled = false
            }
        }
        viewModel.viewStateData.observe(viewLifecycleOwner) { viewState ->
            UiHelpers.setTextOrHide(binding.headerLabel, viewState.headerLabel)
            UiHelpers.setImageOrHide(binding.illustration, viewState.illustration)
            UiHelpers.setTextOrHide(binding.hintLabel, viewState.hintLabel)
            UiHelpers.setTextOrHide(binding.primaryActionBtn, viewState.primaryActionLabel)
            UiHelpers.setTextOrHide(binding.secondaryActionBtn, viewState.secondaryActionLabel)
            binding.primaryActionBtn.setOnClickListener {
                viewState.onPrimaryActionClicked?.invoke()
            }
            binding.secondaryActionBtn.setOnClickListener {
                viewState.onSecondaryActionClicked?.invoke()
            }
        }
    }
}
