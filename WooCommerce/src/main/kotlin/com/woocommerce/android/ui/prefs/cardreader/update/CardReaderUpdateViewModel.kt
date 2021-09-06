package com.woocommerce.android.ui.prefs.cardreader.update

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_READER_SOFTWARE_UPDATE_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_READER_SOFTWARE_UPDATE_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.InstallationStarted
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Installing
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Success
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Unknown
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult.FAILED
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult.SUCCESS
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.ButtonState
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.StateWithProgress
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.UpdatingCancelingState
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.UpdatingState
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PERCENT_100: Int = 100

@HiltViewModel
@Suppress("TooManyFunctions")
class CardReaderUpdateViewModel @Inject constructor(
    private val cardReaderManager: CardReaderManager,
    private val tracker: AnalyticsTrackerWrapper,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val viewState = MutableLiveData<ViewState>()
    val viewStateData: LiveData<ViewState> = viewState

    private val navArgs: CardReaderUpdateDialogFragmentArgs by savedState.navArgs()

    init {
        launch {
            if (navArgs.requiredUpdate.not()) {
                cardReaderManager.installSoftwareUpdate()
            }

            listenToSoftwareUpdateStatus()
        }
    }

    fun onBackPressed() {
        return when (val currentState = viewState.value) {
            is UpdatingState -> showCancelAnywayButton(currentState)
            is UpdatingCancelingState -> viewState.value = buildUpdateState(currentState.progressText)
            else -> triggerEvent(ExitWithResult(FAILED))
        }
    }

    private suspend fun listenToSoftwareUpdateStatus() {
        cardReaderManager.softwareUpdateStatus.collect { status ->
            when (status) {
                is Failed -> onUpdateFailed(status)
                is InstallationStarted -> updateProgress(viewState.value, 0)
                is Installing -> updateProgress(viewState.value, convertProgressToPercentage(status.progress))
                Success -> onUpdateSucceeded()
                Unknown -> onUpdateStatusUnknown()
            }.exhaustive
        }
    }

    private fun onUpdateStatusUnknown() {
        tracker.track(
            CARD_READER_SOFTWARE_UPDATE_FAILED,
            this@CardReaderUpdateViewModel.javaClass.simpleName,
            null,
            "Unknown software update status"
        )
        finishFlow(UpdateResult.SKIPPED)
    }

    private fun onUpdateSucceeded() {
        tracker.track(CARD_READER_SOFTWARE_UPDATE_SUCCESS)
        finishFlow(SUCCESS)
    }

    private fun onUpdateFailed(status: Failed) {
        tracker.track(
            CARD_READER_SOFTWARE_UPDATE_FAILED,
            this@CardReaderUpdateViewModel.javaClass.simpleName,
            null,
            status.message
        )
        finishFlow(FAILED)
    }

    private fun finishFlow(result: UpdateResult) {
        triggerEvent(ExitWithResult(result))
    }

    private fun updateProgress(currentState: ViewState?, progress: Int) {
        if (currentState is StateWithProgress<*>) {
            viewState.value = currentState.copyWithUpdatedProgress(buildProgressText(progress))
        } else {
            viewState.value = buildUpdateState(buildProgressText(progress))
        }
    }

    private fun showCancelAnywayButton(currentState: UpdatingState) {
        viewState.value = UpdatingCancelingState(
            progressText = currentState.progressText,
            button = ButtonState(
                ::onCancelClicked,
                UiStringRes(R.string.cancel_anyway)
            ),
            description = UiStringRes(getWarningDescriptionStringRes()),
        )
    }

    private fun onCancelClicked() {
        tracker.track(
            CARD_READER_SOFTWARE_UPDATE_FAILED,
            this@CardReaderUpdateViewModel.javaClass.simpleName,
            null,
            "User manually cancelled the flow"
        )
        triggerEvent(ExitWithResult(FAILED))
    }

    private fun buildUpdateState(progressText: UiString) =
        UpdatingState(
            progressText = progressText,
            button = ButtonState(
                ::onCancelClicked,
                UiStringRes(R.string.cancel)
            ),
        )

    private fun convertProgressToPercentage(progress: Float) = (progress * PERCENT_100).toInt()

    private fun getWarningDescriptionStringRes() =
        if (navArgs.requiredUpdate) {
            R.string.card_reader_software_update_progress_cancel_required_warning
        } else {
            R.string.card_reader_software_update_progress_cancel_warning
        }

    private fun buildProgressText(progress: Int) =
        UiStringRes(
            R.string.card_reader_software_update_progress_indicator,
            listOf(UiStringText(progress.toString()))
        )

    sealed class ViewState(
        val title: UiString? = null,
        @DrawableRes val illustration: Int = R.drawable.img_card_reader_update_progress,
        open val description: UiString? = null,
        open val progressText: UiString? = null,
        open val button: ButtonState? = null,
    ) {
        data class UpdatingState(
            override val progressText: UiString,
            override val button: ButtonState,
            override val description: UiStringRes = UiStringRes(
                R.string.card_reader_software_update_description
            ),
        ) : StateWithProgress<UpdatingState>, ViewState(
            title = UiStringRes(R.string.card_reader_software_update_in_progress_title),
        ) {
            override fun copyWithUpdatedProgress(progressText: UiString): UpdatingState {
                return this.copy(progressText = progressText)
            }
        }

        data class UpdatingCancelingState(
            override val progressText: UiString,
            override val button: ButtonState,
            override val description: UiStringRes,
        ) : StateWithProgress<UpdatingCancelingState>, ViewState(
            title = UiStringRes(R.string.card_reader_software_update_in_progress_title),
        ) {
            override fun copyWithUpdatedProgress(progressText: UiString): UpdatingCancelingState {
                return this.copy(progressText = progressText)
            }
        }

        data class ButtonState(
            val onActionClicked: (() -> Unit),
            val text: UiString
        )

        interface StateWithProgress<T : ViewState> {
            fun copyWithUpdatedProgress(progressText: UiString): T
        }
    }

    enum class UpdateResult {
        SUCCESS, SKIPPED, FAILED
    }
}
