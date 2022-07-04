package com.woocommerce.android.ui.cardreader.update

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.CardReaderUpdateDialogBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.CardReaderUpdateEvent.SoftwareUpdateAboutToStart
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.CardReaderUpdateEvent.SoftwareUpdateProgress
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.UpdateResult
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderUpdateDialogFragment : DialogFragment(R.layout.card_reader_update_dialog) {
    val viewModel: CardReaderUpdateViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        with(requireDialog()) {
            this.setCanceledOnTouchOutside(false)
            this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                viewModel.onBackPressed()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = CardReaderUpdateDialogBinding.bind(view)

        initObservers(binding)
    }

    private fun initObservers(binding: CardReaderUpdateDialogBinding) {
        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    is ExitWithResult<*> -> navigateBackWithResult(
                        KEY_READER_UPDATE_RESULT,
                        event.data as UpdateResult
                    )
                    is SoftwareUpdateProgress ->
                        announceSoftwareUpdateProgress(event.progress, binding)
                    is SoftwareUpdateAboutToStart ->
                        binding.root.announceForAccessibility(getString(event.accessibilityText))
                    else -> event.isHandled = false
                }
            }
        )

        viewModel.viewStateData.observe(
            viewLifecycleOwner,
            { state ->
                with(binding) {
                    UiHelpers.setTextOrHide(titleTextView, state.title)
                    UiHelpers.setTextOrHide(descriptionTextView, state.description)
                    UiHelpers.setTextOrHide(progressTextView, state.progressText)
                    UiHelpers.setTextOrHide(actionButton, state.button?.text)
                    with(progressCircleProgressOverlayView) {
                        UiHelpers.updateVisibility(this, state.progress != null)
                        currentProgressPercentage = state.progress ?: 0
                    }
                    UiHelpers.setImageOrHideInLandscape(progressImageView, state.illustration)
                    actionButton.setOnClickListener { state.button?.onActionClicked?.invoke() }
                }
            }
        )
    }

    private fun announceSoftwareUpdateProgress(
        progressText: UiString,
        binding: CardReaderUpdateDialogBinding
    ) {
        val progress = UiHelpers.getTextOfUiString(requireActivity(), progressText)
        binding.progressTextView.announceForAccessibility(progress)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    companion object {
        const val KEY_READER_UPDATE_RESULT = "key_reader_update_result"
    }
}
