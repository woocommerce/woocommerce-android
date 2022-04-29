package com.woocommerce.android.ui.cardreader.update

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.InstallationStarted
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Installing
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Success
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Unknown
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatusErrorType
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.cardreader.CardReaderTracker
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.CardReaderUpdateEvent.SoftwareUpdateAboutToStart
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.UpdateResult.FAILED
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.UpdateResult.SUCCESS
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.ViewState.ButtonState
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.ViewState.StateWithProgress
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.ViewState.UpdateAboutToStart
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.ViewState.UpdateFailedBatteryLow
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.ViewState.UpdatingCancelingState
import com.woocommerce.android.ui.cardreader.update.CardReaderUpdateViewModel.ViewState.UpdatingState
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PERCENT_100: Int = 100

@HiltViewModel
class CardReaderUpdateViewModel @Inject constructor(
    private val cardReaderManager: CardReaderManager,
    private val tracker: CardReaderTracker,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val viewState = MutableLiveData<ViewState>()
    val viewStateData: LiveData<ViewState> = viewState

    private val navArgs: CardReaderUpdateDialogFragmentArgs by savedState.navArgs()

    init {
        tracker.trackSoftwareUpdateStarted(navArgs.requiredUpdate)
        launch {
            if (navArgs.requiredUpdate.not()) {
                cardReaderManager.startAsyncSoftwareUpdate()
            }
            listenToSoftwareUpdateStatus()
        }
    }

    fun onBackPressed() {
        return when (val currentState = viewState.value) {
            is UpdatingState -> showCancelAnywayButton(currentState)
            is UpdatingCancelingState -> viewState.value = UpdatingState(
                progress = currentState.progress,
                progressText = currentState.progressText
            )
            is UpdateAboutToStart -> {
                // noop. Update is initiating, we can not cancel it
            }
            else -> finishFlow(FAILED)
        }
    }

    private suspend fun listenToSoftwareUpdateStatus() {
        cardReaderManager.softwareUpdateStatus.collect { status ->
            when (status) {
                is Failed -> onUpdateFailed(status)
                is InstallationStarted -> {
                    triggerEvent(SoftwareUpdateAboutToStart(R.string.card_reader_software_update_description))
                    viewState.value = UpdateAboutToStart(
                        buildProgressText(convertToPercentage(0f))
                    )
                }
                is Installing -> {
                    triggerEvent(
                        CardReaderUpdateEvent.SoftwareUpdateProgress(
                            buildProgressText(
                                convertToPercentage(status.progress)
                            )
                        )
                    )
                    updateProgress(viewState.value, convertToPercentage(status.progress))
                }
                Success -> onUpdateSucceeded()
                Unknown -> onUpdateStatusUnknown()
            }.exhaustive
        }
    }

    private fun onUpdateStatusUnknown() {
        tracker.trackSoftwareUpdateUnknownStatus()
    }

    private fun onUpdateSucceeded() {
        tracker.trackSoftwareUpdateSucceeded(navArgs.requiredUpdate)
        finishFlow(SUCCESS)
    }

    private fun onUpdateFailed(status: Failed) {
        tracker.trackSoftwareUpdateFailed(status, navArgs.requiredUpdate)
        val errorType = status.errorType
        when (errorType) {
            is SoftwareUpdateStatusErrorType.BatteryLow ->
                viewState.value = UpdateFailedBatteryLow(
                    description = getLowBatteryErrorDescription(errorType.currentBatteryLevel),
                    button = ButtonState(
                        { finishFlow(FAILED) },
                        UiStringRes(R.string.cancel)
                    ),
                )
            else -> finishFlow(FAILED)
        }.exhaustive
    }

    private fun finishFlow(result: UpdateResult) {
        triggerEvent(ExitWithResult(result))
    }

    private fun updateProgress(currentState: ViewState?, progress: Int) {
        if (currentState is StateWithProgress<*>) {
            viewState.value = currentState.copyWithUpdatedProgress(
                progress,
                buildProgressText(progress)
            )
        } else {
            viewState.value = UpdatingState(
                progress = progress,
                progressText = buildProgressText(progress)
            )
        }
    }

    private fun showCancelAnywayButton(currentState: UpdatingState) {
        viewState.value = UpdatingCancelingState(
            progress = currentState.progress,
            progressText = currentState.progressText,
            button = ButtonState(
                ::onCancelClicked,
                UiStringRes(R.string.cancel_anyway)
            ),
            description = UiStringRes(getWarningDescriptionStringRes()),
        )
    }

    private fun onCancelClicked() {
        cardReaderManager.cancelOngoingFirmwareUpdate()
        tracker.trackSoftwareUpdateCancelled(navArgs.requiredUpdate)
        triggerEvent(ExitWithResult(FAILED))
    }

    private fun convertToPercentage(progress: Float) = (progress * PERCENT_100).toInt()

    private fun getWarningDescriptionStringRes() =
        if (navArgs.requiredUpdate) {
            R.string.card_reader_software_update_progress_cancel_required_warning
        } else {
            R.string.card_reader_software_update_progress_cancel_warning
        }

    private fun getLowBatteryErrorDescription(currentBatteryLevel: Float?): UiStringRes {
        return if (currentBatteryLevel != null) {
            UiStringRes(
                R.string.card_reader_software_update_progress_description_low_battery,
                listOf(UiStringText(convertToPercentage(currentBatteryLevel).toString()))
            )
        } else {
            UiStringRes(R.string.card_reader_software_update_progress_description_low_battery_level_unknown)
        }
    }

    private fun buildProgressText(progress: Int) =
        UiStringRes(
            R.string.card_reader_software_update_progress_indicator,
            listOf(UiStringText(progress.toString()))
        )

    sealed class CardReaderUpdateEvent : Event() {
        data class SoftwareUpdateProgress(val progress: UiString) : Event()
        data class SoftwareUpdateAboutToStart(
            @StringRes val accessibilityText: Int
        ) : Event()
    }

    sealed class ViewState(
        val title: UiString? = null,
        @DrawableRes val illustration: Int = R.drawable.img_card_reader_update_progress,
        open val description: UiString? = null,
        open val progress: Int? = null,
        open val progressText: UiString? = null,
        open val button: ButtonState? = null,
    ) {
        data class UpdateAboutToStart(
            override val progressText: UiString,
        ) : ViewState(
            progress = 0,
            title = UiStringRes(R.string.card_reader_software_update_in_progress_title),
            description = UiStringRes(R.string.card_reader_software_update_description),
        )

        data class UpdatingState(
            override val progress: Int,
            override val progressText: UiString,
            override val description: UiStringRes = UiStringRes(
                R.string.card_reader_software_update_description
            ),
        ) : StateWithProgress<UpdatingState>, ViewState(
            title = UiStringRes(R.string.card_reader_software_update_in_progress_title),
        ) {
            override fun copyWithUpdatedProgress(progress: Int, progressText: UiString): UpdatingState {
                return this.copy(
                    progress = progress,
                    progressText = progressText
                )
            }
        }

        data class UpdatingCancelingState(
            override val progress: Int,
            override val progressText: UiString,
            override val button: ButtonState,
            override val description: UiStringRes,
        ) : StateWithProgress<UpdatingCancelingState>, ViewState(
            title = UiStringRes(R.string.card_reader_software_update_in_progress_title),
        ) {
            override fun copyWithUpdatedProgress(progress: Int, progressText: UiString): UpdatingCancelingState {
                return this.copy(
                    progress = progress,
                    progressText = progressText
                )
            }
        }

        data class UpdateFailedBatteryLow(
            override val description: UiString,
            override val button: ButtonState,
        ) : ViewState(
            title = UiStringRes(R.string.card_reader_software_update_title_battery_low),
            illustration = R.drawable.img_card_reader_update_failed_battery_low,
        )

        data class ButtonState(
            val onActionClicked: (() -> Unit),
            val text: UiString
        )

        interface StateWithProgress<T : ViewState> {
            fun copyWithUpdatedProgress(progress: Int, progressText: UiString): T
        }
    }

    enum class UpdateResult {
        SUCCESS, FAILED
    }
}
