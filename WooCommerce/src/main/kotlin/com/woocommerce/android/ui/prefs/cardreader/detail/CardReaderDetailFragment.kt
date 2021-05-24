package com.woocommerce.android.ui.prefs.cardreader.detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.databinding.FragmentCardReaderDetailBinding
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.setDrawableColor
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderConnectScreen
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.ConnectedState
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.Loading
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.NotConnectedState
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
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
        viewModel.event.observe(viewLifecycleOwner, { event ->
            when (event) {
                is CardReaderConnectScreen ->
                    findNavController()
                        .navigateSafely(R.id.action_cardReaderDetailFragment_to_cardReaderConnectFragment)
                is ShowSnackbar -> {
                    Snackbar.make(
                        binding.root,
                        getString(event.message),
                        BaseTransientBottomBar.LENGTH_LONG
                    ).show()
                }
                else ->
                    event.isHandled = false
            }
        })

        viewModel.viewStateData.observe(viewLifecycleOwner, { state ->
            makeStateVisible(binding, state)
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
                        binding.readerConnectedState.enforcedUpdateTv.setDrawableColor(color.woo_red_50)
                    }
                }
                is NotConnectedState -> {
                    with(binding.readerDisconnectedState) {
                        UiHelpers.setTextOrHide(cardReaderDetailConnectHeaderLabel, state.headerLabel)
                        UiHelpers.setImageOrHide(cardReaderDetailIllustration, state.illustration)
                        UiHelpers.setTextOrHide(cardReaderDetailFirstHintLabel, state.firstHintLabel)
                        UiHelpers.setTextOrHide(cardReaderDetailSecondHintLabel, state.secondHintLabel)
                        UiHelpers.setTextOrHide(cardReaderDetailConnectBtn, state.connectBtnLabel)
                        cardReaderDetailConnectBtn.setOnClickListener { state.onPrimaryActionClicked.invoke() }
                    }
                }
                Loading -> {
                }
            }.exhaustive
        })
    }

    private fun makeStateVisible(binding: FragmentCardReaderDetailBinding, state: ViewState) {
        UiHelpers.updateVisibility(binding.readerConnectedState.root, state is ConnectedState)
        UiHelpers.updateVisibility(binding.readerDisconnectedState.root, state is NotConnectedState)
        UiHelpers.updateVisibility(binding.readerConnectedLoading, state is Loading)
    }
}
