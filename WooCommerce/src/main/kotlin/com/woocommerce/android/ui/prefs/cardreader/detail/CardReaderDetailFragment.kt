package com.woocommerce.android.ui.prefs.cardreader.detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderDetailBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.setDrawableColor
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderConnectScreen
import com.woocommerce.android.util.UiHelpers
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderDetailFragment : BaseFragment(R.layout.fragment_card_reader_detail) {
    val viewModel: CardReaderDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCardReaderDetailBinding.bind(view)

        initUI(binding)
        initObservers(binding)
    }

    private fun initUI(binding: FragmentCardReaderDetailBinding) {
        binding.readerConnectedState.enforcedUpdateTv.setDrawableColor(R.color.woo_red_30)
    }

    private fun initObservers(binding: FragmentCardReaderDetailBinding) {
        viewModel.event.observe(viewLifecycleOwner, {
            when (it) {
                is CardReaderConnectScreen ->
                    findNavController()
                        .navigateSafely(R.id.action_cardReaderDetailFragment_to_cardReaderConnectFragment)
                else ->
                    it.isHandled = false
            }
        })

        viewModel.viewStateData.observe(viewLifecycleOwner, {
            UiHelpers.setTextOrHide(binding.readerDisconnectedState.headerLabel, it.headerLabel)
            UiHelpers.setImageOrHide(binding.readerDisconnectedState.illustration, it.illustration)
            UiHelpers.setTextOrHide(binding.readerDisconnectedState.firstHintLabel, it.firstHintLabel)
            UiHelpers.setTextOrHide(binding.readerDisconnectedState.secondHintLabel, it.secondHintLabel)
            UiHelpers.setTextOrHide(binding.readerDisconnectedState.connectBtn, it.connectBtnLabel)
            binding.readerDisconnectedState.connectBtn.setOnClickListener { _ -> it.onPrimaryActionClicked?.invoke() }
        })
    }
}
