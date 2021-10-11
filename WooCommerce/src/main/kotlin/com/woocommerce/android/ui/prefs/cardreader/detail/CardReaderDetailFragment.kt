package com.woocommerce.android.ui.prefs.cardreader.detail

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentCardReaderDetailBinding
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.setDrawableColor
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.CardReaderDetailEvent.CopyReadersNameToClipboard
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderConnectScreen
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderUpdateScreen
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.ConnectedState
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.Loading
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.NotConnectedState
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateDialogFragment
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.util.copyToClipboard
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderDetailFragment : BaseFragment(R.layout.fragment_card_reader_detail) {
    val viewModel: CardReaderDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCardReaderDetailBinding.bind(view)

        val learnMoreListener = View.OnClickListener {
            ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
        }
        binding.readerConnectedState.cardReaderDetailLearnMoreTv.learnMore.setOnClickListener(learnMoreListener)
        binding.readerDisconnectedState.cardReaderDetailLearnMoreTv.learnMore.setOnClickListener(learnMoreListener)

        observeEvents(binding)
        observeViewState(binding)
        initResultHandlers()
    }

    private fun observeEvents(binding: FragmentCardReaderDetailBinding) {
        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    is CardReaderConnectScreen ->
                        findNavController()
                            .navigateSafely(
                                CardReaderDetailFragmentDirections
                                    .actionCardReaderDetailFragmentToCardReaderConnectFragment(skipOnboarding = true)
                            )
                    is CardReaderUpdateScreen ->
                        findNavController().navigateSafely(
                            CardReaderDetailFragmentDirections
                                .actionCardReaderDetailFragmentToCardReaderUpdateDialogFragment(event.startedByUser)
                        )
                    is ShowSnackbar -> {
                        Snackbar.make(
                            binding.root,
                            getString(event.message),
                            BaseTransientBottomBar.LENGTH_LONG
                        ).show()
                    }
                    is CopyReadersNameToClipboard -> requireContext().copyToClipboard(
                        event.readersName,
                        event.readersName
                    )
                    else -> event.isHandled = false
                }
            }
        )
    }

    private fun observeViewState(binding: FragmentCardReaderDetailBinding) {
        viewModel.viewStateData.observe(
            viewLifecycleOwner,
            { state ->
                makeStateVisible(binding, state)
                when (state) {
                    is ConnectedState -> {
                        with(binding.readerConnectedState) {
                            UiHelpers.setTextOrHide(enforcedUpdateTv, state.enforceReaderUpdate)
                            enforcedUpdateDivider.visibility = enforcedUpdateTv.visibility
                            with(readerNameTv) {
                                UiHelpers.setTextOrHide(readerNameTv, state.readerName)
                                setOnLongClickListener { state.onReaderNameLongClick(); true }
                            }
                            UiHelpers.setTextOrHide(readerBatteryTv, state.readerBattery)
                            UiHelpers.setTextOrHide(readerFirmwareVersionTv, state.readerFirmwareVersion)
                            UiHelpers.setTextOrHide(primaryActionBtn, state.primaryButtonState?.text)
                            primaryActionBtn.setOnClickListener { state.primaryButtonState?.onActionClicked?.invoke() }
                            UiHelpers.setTextOrHide(secondaryActionBtn, state.secondaryButtonState?.text)
                            secondaryActionBtn.setOnClickListener {
                                state.secondaryButtonState?.onActionClicked?.invoke()
                            }
                            binding.readerConnectedState.enforcedUpdateTv.setDrawableColor(
                                color.warning_banner_foreground_color
                            )
                            with(cardReaderDetailLearnMoreTv.root) {
                                movementMethod = LinkMovementMethod.getInstance()
                                UiHelpers.setTextOrHide(this, state.learnMoreLabel)
                            }
                        }
                    }
                    is NotConnectedState -> {
                        with(binding.readerDisconnectedState) {
                            UiHelpers.setTextOrHide(cardReaderDetailConnectHeaderLabel, state.headerLabel)
                            UiHelpers.setImageOrHide(cardReaderDetailIllustration, state.illustration)
                            UiHelpers.setTextOrHide(cardReaderDetailFirstHintLabel, state.firstHintLabel)
                            UiHelpers.setTextOrHide(cardReaderDetailFirstHintNumberLabel, state.firstHintNumber)
                            UiHelpers.setTextOrHide(cardReaderDetailSecondHintLabel, state.secondHintLabel)
                            UiHelpers.setTextOrHide(cardReaderDetailSecondHintNumberLabel, state.secondHintNumber)
                            UiHelpers.setTextOrHide(cardReaderDetailThirdHintLabel, state.thirdHintLabel)
                            UiHelpers.setTextOrHide(cardReaderDetailThirdHintNumberLabel, state.thirdHintNumber)
                            UiHelpers.setTextOrHide(cardReaderDetailConnectBtn, state.connectBtnLabel)
                            cardReaderDetailConnectBtn.setOnClickListener { state.onPrimaryActionClicked.invoke() }
                            with(cardReaderDetailLearnMoreTv.root) {
                                movementMethod = LinkMovementMethod.getInstance()
                                UiHelpers.setTextOrHide(this, state.learnMoreLabel)
                            }
                        }
                    }
                    Loading -> {
                    }
                }.exhaustive
            }
        )
    }

    private fun initResultHandlers() {
        handleResult<UpdateResult>(CardReaderUpdateDialogFragment.KEY_READER_UPDATE_RESULT) {
            viewModel.onUpdateReaderResult(it)
        }
    }

    private fun makeStateVisible(binding: FragmentCardReaderDetailBinding, state: ViewState) {
        UiHelpers.updateVisibility(binding.readerConnectedState.root, state is ConnectedState)
        UiHelpers.updateVisibility(binding.readerDisconnectedState.root, state is NotConnectedState)
        UiHelpers.updateVisibility(binding.readerConnectedLoading, state is Loading)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
