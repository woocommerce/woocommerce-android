package com.woocommerce.android.ui.prefs.cardreader.update

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.SoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.SoftwareUpdateStatus.Initializing
import com.woocommerce.android.cardreader.SoftwareUpdateStatus.Installing
import com.woocommerce.android.cardreader.SoftwareUpdateStatus.Success
import com.woocommerce.android.cardreader.SoftwareUpdateStatus.UpToDate
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateDialogFragment.Companion.KEY_SKIP_UPDATE
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.ButtonState
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.ExplanationState
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.UpdatingState
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardReaderUpdateViewModel @Inject constructor(
    private val cardReaderManager: CardReaderManager,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val viewState = MutableLiveData<ViewState>()
    val viewStateData: LiveData<ViewState> = viewState

    private val skipUpdate = savedState.get<Boolean>(KEY_SKIP_UPDATE)!!

    init {
        viewState.value = ExplanationState(
            primaryButton = ButtonState(
                onActionClicked = ::startUpdate,
                text = UiStringRes(R.string.card_reader_software_update_update)
            ),
            secondaryButton = ButtonState(
                onActionClicked = ::finishFlow,
                text = UiStringRes(
                    if (skipUpdate) R.string.card_reader_software_update_skip
                    else R.string.card_reader_software_update_cancel
                )
            )
        )
    }

    private fun startUpdate() {
        launch {
            cardReaderManager.updateSoftware().collect { status ->
                when (status) {
                    is Failed -> viewState.value = ViewState.ErrorState
                    Initializing, is Installing -> viewState.value = UpdatingState
                    Success, UpToDate -> {
                        showUpdateFinished()
                        finishFlow()
                    }
                }.exhaustive
            }
        }
    }

    private fun finishFlow() {
        triggerEvent(Event.Exit)
    }

    private fun showUpdateFinished() {
        triggerEvent(Event.ShowSnackbar(R.string.card_reader_software_update_success))
    }

    sealed class ViewState(
        val title: UiString? = null,
        val description: UiString? = null,
        val showProgress: Boolean = false,
        open val primaryButton: ButtonState? = null,
        open val secondaryButton: ButtonState? = null,
    ) {
        data class ExplanationState(
            override val primaryButton: ButtonState?,
            override val secondaryButton: ButtonState?
        ) : ViewState(
            title = UiStringRes(R.string.card_reader_software_update_title),
            description = UiStringRes(R.string.card_reader_software_update_description),
        )

        // todo cardreader implement when design is ready
        object ErrorState : ViewState()

        object UpdatingState : ViewState(
            title = UiStringRes(R.string.card_reader_software_update_in_progress_title),
            showProgress = true
        )

        data class ButtonState(
            val onActionClicked: (() -> Unit),
            val text: UiString
        )
    }
}
