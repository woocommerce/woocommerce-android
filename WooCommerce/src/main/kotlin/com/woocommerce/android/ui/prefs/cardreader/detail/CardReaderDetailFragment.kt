package com.woocommerce.android.ui.prefs.cardreader.detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderDetailBinding
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.setDrawableColor
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderConnectScreen
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.ConnectedState
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.NotConnectedState
import com.woocommerce.android.util.UiHelpers
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderDetailFragment : BaseFragment(R.layout.fragment_card_reader_detail) {
    val viewModel: CardReaderDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCardReaderDetailBinding.bind(view)

        initObservers(binding)
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

        viewModel.viewStateData.observe(viewLifecycleOwner, { state ->
            UiHelpers.updateVisibility(binding.readerConnectedState.root, state is ConnectedState)
            UiHelpers.updateVisibility(binding.readerDisconnectedState.root, state is NotConnectedState)

            when (state) {
                is ConnectedState -> {
                    with(binding.readerConnectedState) {
                        UiHelpers.setTextOrHide(enforcedUpdateTv, state.enforceReaderUpdate)
                        UiHelpers.setTextOrHide(readerNameTv, state.readerName)
                        UiHelpers.setTextOrHide(readerBatteryTv, state.readerBattery)
                        UiHelpers.setTextOrHide(primaryActionBtn, state.primaryButtonState?.text)
                        primaryActionBtn.setOnClickListener { state.primaryButtonState?.onActionClicked?.invoke() }
                        UiHelpers.setTextOrHide(secondaryActionBtn, state.secondaryButtonState?.text)
                        secondaryActionBtn.setOnClickListener { state.secondaryButtonState?.onActionClicked?.invoke() }
                        binding.readerConnectedState.enforcedUpdateTv.setDrawableColor(R.color.woo_red_50)
                    }
                }
                is NotConnectedState -> {
                    viewModel.viewStateData.observe(viewLifecycleOwner, Observer {
                        UiHelpers.setTextOrHide(binding.cardReaderDetailConnectHeaderLabel, it.headerLabel)
                        UiHelpers.setImageOrHide(binding.cardReaderDetailIllustration, it.illustration)
                        UiHelpers.setTextOrHide(binding.cardReaderDetailFirstHintLabel, it.firstHintLabel)
                        UiHelpers.setTextOrHide(binding.cardReaderDetailSecondHintLabel, it.secondHintLabel)
                        UiHelpers.setTextOrHide(binding.cardReaderDetailConnectBtn, it.connectBtnLabel)
                        binding.cardReaderDetailConnectBtn.setOnClickListener { _ -> it.onPrimaryActionClicked?.invoke() }
                    })
                }
            }.exhaustive
        })
    }
}
