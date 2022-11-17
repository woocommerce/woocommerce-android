package com.woocommerce.android.ui.prefs

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.SpinnerListItem
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.ToggleableListItem
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.UpdateOptions
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeveloperOptionsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val developerOptionsRepository: DeveloperOptionsRepository,
) : ScopedViewModel(savedState) {

    private val _viewState = MutableLiveData(
        DeveloperOptionsViewState(
            rows = (
                createDeveloperOptionsList()
                )
        )
    )

    val viewState: LiveData<DeveloperOptionsViewState> = _viewState

    private fun createDeveloperOptionsList(): List<ListItem> = mutableListOf(
        ToggleableListItem(
            icon = drawable.img_card_reader_connecting,
            label = UiStringRes(string.enable_card_reader),
            key = UiStringRes(string.simulated_reader_key),
            isEnabled = true,
            isChecked = developerOptionsRepository.isSimulatedCardReaderEnabled(),
            onToggled = ::onSimulatedReaderToggled
        ),
        SpinnerListItem(
            icon = drawable.img_card_reader_update_progress,
            endIcon = drawable.ic_arrow_drop_down,
            label = UiStringRes(string.update_simulated_reader),
            key = UiStringRes(string.update_simulated_reader_key),
            isEnabled = true,
            onClick = ::onUpdateSimulatedReaderClicked,
        )
    )

    private fun onSimulatedReaderToggled(isChecked: Boolean) {
        if (!isChecked) {
            disconnectAndClearSelectedCardReader()
            triggerEvent(
                DeveloperOptionsEvents.ShowToastString(string.simulated_reader_toast)
            )
        }
        simulatedReaderStateChanged(isChecked)
    }

    private fun disconnectAndClearSelectedCardReader() {
        launch {
            developerOptionsRepository.clearSelectedCardReader()
        }
    }

    private fun simulatedReaderStateChanged(isChecked: Boolean) {
        developerOptionsRepository.changeSimulatedReaderState(isChecked)
        val currentViewState = viewState.value
        (
            currentViewState?.rows?.find {
                it.key == UiStringRes(string.simulated_reader_key)
            } as? ToggleableListItem
            )?.let { originalListItem ->
            val newState = originalListItem.copy(isChecked = isChecked)
            _viewState.value = currentViewState.copy(
                rows = currentViewState.rows.map {
                    if (it.label == newState.label)
                        newState
                    else it
                }
            )
        }
    }

    private fun onUpdateSimulatedReaderClicked() {
        triggerEvent(
            DeveloperOptionsEvents.ShowUpdateOptionsDialog(
                UpdateOptions.values().toList(),
                developerOptionsRepository.getUpdateSimulatedReaderOption()
            )
        )
    }

    fun onUpdateReaderOptionChanged(selectedOption: UpdateOptions) {
        developerOptionsRepository.updateSimulatedReaderOption(selectedOption)
    }

    sealed class DeveloperOptionsEvents : MultiLiveEvent.Event() {
        data class ShowToastString(val message: Int) : DeveloperOptionsEvents()
        data class ShowUpdateOptionsDialog(
            val options: List<UpdateOptions>,
            var selectedValue: UpdateOptions,
        ) : DeveloperOptionsEvents()
    }

    data class DeveloperOptionsViewState(
        val rows: List<ListItem>
    ) {
        sealed class ListItem {
            abstract val label: UiString
            abstract val icon: Int?
            abstract var isEnabled: Boolean
            abstract var key: UiString

            data class ToggleableListItem(
                @DrawableRes override val icon: Int,
                override val label: UiString,
                override var isEnabled: Boolean = false,
                override var key: UiString,
                val onToggled: (Boolean) -> Unit,
                val isChecked: Boolean
            ) : ListItem()

            data class NonToggleableListItem(
                @DrawableRes override val icon: Int,
                override val label: UiString,
                override var isEnabled: Boolean = false,
                override var key: UiString,
                val onClick: () -> Unit
            ) : ListItem()

            data class SpinnerListItem(
                @DrawableRes override val icon: Int,
                @DrawableRes val endIcon: Int,
                override val label: UiString,
                override var isEnabled: Boolean = false,
                override var key: UiString,
                val onClick: () -> Unit,

            ) : ListItem()
        }

        enum class UpdateOptions(@StringRes val title: Int) {
            ALWAYS(string.always_update_reader),
            NEVER(string.never_update_reader),
            RANDOMLY(string.randomly_update_reader)
        }
    }
}
